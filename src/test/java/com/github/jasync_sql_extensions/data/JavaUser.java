package com.github.jasync_sql_extensions.data;

import java.util.Objects;

public class JavaUser {
    private final Long id;
    private final String name;
    private final String shortName;

    public JavaUser(Long id, String name, String shortName) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
    }

    /**
     * @return the id of the {@link JavaUser}
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the name of the {@link JavaUser}
     */
    public String getName() {
        return name;
    }

    /**
     * @return the shortName of the {@link JavaUser}
     */
    public String getShortName() {
        return shortName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaUser javaUser = (JavaUser) o;
        return Objects.equals(id, javaUser.id) &&
                Objects.equals(name, javaUser.name) &&
                Objects.equals(shortName, javaUser.shortName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, shortName);
    }
}
