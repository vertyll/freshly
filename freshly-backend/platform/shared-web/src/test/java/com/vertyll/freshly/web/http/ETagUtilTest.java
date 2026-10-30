package com.vertyll.freshly.web.http;

import org.junit.jupiter.api.Test;

import com.vertyll.freshly.lang.error.DomainException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ETagUtilTest {
    @Test
    void aVersionBecomesAWeakTag() {
        assertThat(ETagUtil.buildWeakETag(7L)).isEqualTo("W/\"7\"");
    }

    @Test
    void anAggregateThatWasNeverSavedHasNoTag() {
        assertThatThrownBy(() -> ETagUtil.buildWeakETag(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aMissingHeaderAsksForNoCheck() {
        assertThat(ETagUtil.parseVersion(null)).isNull();
    }

    @Test
    void aStarAsksForNoCheck() {
        assertThat(ETagUtil.parseVersion(" * ")).isNull();
    }

    @Test
    void aWeakTagGivesItsVersion() {
        assertThat(ETagUtil.parseVersion("W/\"7\"")).isEqualTo(7L);
    }

    @Test
    void aStrongTagGivesItsVersion() {
        assertThat(ETagUtil.parseVersion("\"7\"")).isEqualTo(7L);
    }

    @Test
    void aTagWithAnythingAfterItIsRefusedRatherThanIgnored() {
        assertThatThrownBy(() -> ETagUtil.parseVersion("\"1\"junk")).isInstanceOfSatisfying(
            DomainException.class,
            e -> assertThat(e.error()).isEqualTo(IfMatchError.MALFORMED)
        );
    }
}
