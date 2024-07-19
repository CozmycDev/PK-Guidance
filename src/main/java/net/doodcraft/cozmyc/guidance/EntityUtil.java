package net.doodcraft.cozmyc.guidance;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class EntityUtil {

    /**
     * Retrieves the entity class based on the given entity name.
     *
     * @param entityName the name of the entity
     * @return the entity class
     */
    public static Class<? extends Entity> getEntityClass(String entityName) {
        try {
            EntityType type = EntityType.valueOf(entityName.toUpperCase());
            return getEntityClass(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid entity name: " + entityName, e);
        }
    }

    /**
     * Retrieves the entity class based on the given entity type.
     *
     * @param entityType the type of the entity
     * @return the entity class
     * @throws IllegalArgumentException if no entity class is found for the given entity type
     */
    public static Class<? extends Entity> getEntityClass(EntityType entityType) {
        Class<? extends Entity> entityClass = entityType.getEntityClass();
        if (entityClass == null) {
            throw new IllegalArgumentException("No entity class found for: " + entityType);
        }
        return entityClass;
    }
}
