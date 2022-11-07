package io.github.portaldalaran.talons.meta;

import lombok.Data;

/**
 * @author aohee@163.com
 */
@Data
public class AssociationQueryField {
    /**
     *
     */
    private String tableName;
    /**
     * sql select parameter name,sex
     */
    private String parameters;
}
