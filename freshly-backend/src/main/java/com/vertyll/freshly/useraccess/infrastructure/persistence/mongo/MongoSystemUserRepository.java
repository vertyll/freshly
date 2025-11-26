package com.vertyll.freshly.useraccess.infrastructure.persistence.mongo;

import com.vertyll.freshly.useraccess.domain.SystemUser;
import com.vertyll.freshly.useraccess.domain.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("systemUserRepository")
@RequiredArgsConstructor
class MongoSystemUserRepository implements SystemUserRepository {

    private final SpringDataSystemUserRepository springDataRepository;
    private final SystemUserMapper mapper;

    @Override
    public SystemUser save(SystemUser user) {
        SystemUserDocument document = mapper.toDocument(user);
        SystemUserDocument saved = springDataRepository.save(document);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<SystemUser> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<SystemUser> findAll() {
        return springDataRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
