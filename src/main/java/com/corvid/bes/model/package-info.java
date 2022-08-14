@FilterDefs({
    @FilterDef(
            name = "filterByDeleted",
            defaultCondition = "deleted = false"
    )
})

package com.corvid.bes.model;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;