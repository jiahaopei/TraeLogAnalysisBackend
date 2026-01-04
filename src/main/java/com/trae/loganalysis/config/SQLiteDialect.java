package com.trae.loganalysis.config;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/**
 * SQLite方言实现，兼容Hibernate 6.x
 */
public class SQLiteDialect extends Dialect {

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new IdentityColumnSupportImpl() {
            @Override
            public boolean supportsIdentityColumns() {
                return true;
            }

            @Override
            public boolean hasDataTypeInIdentityColumn() {
                return false;
            }

            @Override
            public String getIdentitySelectString(String table, String column, int type) {
                return "select last_insert_rowid()";
            }

            @Override
            public String getIdentityColumnString(int type) {
                return "primary key autoincrement";
            }
        };
    }

    @Override
    public boolean hasAlterTable() {
        // SQLite doesn't support ALTER TABLE, it has limited support
        return false;
    }

    @Override
    public boolean dropConstraints() {
        // SQLite doesn't support constraints dropping
        return false;
    }

    @Override
    public boolean supportsIfExistsBeforeTableName() {
        return true;
    }

    @Override
    public boolean supportsIfExistsBeforeConstraintName() {
        return false;
    }
}
