package com.example.memgptagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public class VersionedEntity {

    @Version
    @Column(nullable = false, name = "version")
    private long version;

    public long getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

}
