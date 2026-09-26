package com.vertyll.freshly.infra.transaction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Puts a transaction around a use case without the use case knowing.
 *
 * <p>
 * The transaction <em>boundary</em> is a use case; the transaction <em>mechanism</em>
 * is infrastructure. Splitting the two is what lets every application layer in the build
 * stay free of {@code @Transactional} — and therefore of Spring altogether, which is the
 * property {@code checkHexagonalDependencies} verifies.
 *
 * <p>
 * A dynamic proxy rather than a handwritten decorator per port: those would be
 * hundreds of lines of pure delegation, and every new use-case method would need a
 * matching edit or would silently run outside a transaction. The proxy has no method to
 * forget.
 *
 * <p>
 * Read-only mode follows the <em>port</em>, not a list of method names. A query port is
 * read-only in its entirety and a command port is not, so the caller passes a constant and
 * nothing hand-maintained can drift out of step with a rename. The version of this class
 * that takes a list of names looks more flexible and is strictly worse.
 */
@Component
public class TransactionalUseCaseFactory {
    private final TransactionTemplate readWrite;
    private final TransactionTemplate readOnly;

    public TransactionalUseCaseFactory(PlatformTransactionManager transactionManager) {
        this.readWrite = new TransactionTemplate(transactionManager);

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setReadOnly(true);
        this.readOnly = template;
    }

    public <T> T readWrite(Class<T> contract, T target) {
        return wrap(contract, target, readWrite);
    }

    public <T> T readOnly(Class<T> contract, T target) {
        return wrap(contract, target, readOnly);
    }

    @SuppressWarnings("PMD.UseProperClassLoader")
    private <T> T wrap(Class<T> contract, T target, TransactionTemplate template) {
        Object proxy = Proxy.newProxyInstance(
            contract.getClassLoader(),
            new Class<?>[] {
                contract
            },
            (instance, method, args) -> invoke(target, template, method, args)
        );
        return contract.cast(proxy);
    }

    @SuppressWarnings("PMD.UseVarargs")
    @Nullable private static Object invoke(
        Object target,
        TransactionTemplate template,
        Method method,
        Object @Nullable [] args
    ) throws IllegalAccessException, InvocationTargetException {
        Object[] arguments = args == null ? new Object[0] : args;

        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, arguments);
        }

        return template.execute(status -> proceed(target, method, arguments));
    }

    @SuppressFBWarnings(
        value = "THROWS_METHOD_THROWS_RUNTIMEEXCEPTION",
        justification = "Rethrows the use case's own exception unwrapped from reflection"
    )
    @Nullable private static Object proceed(Object target, Method method, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException e) {
            throw sneakyThrow(e.getTargetException());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }
}
