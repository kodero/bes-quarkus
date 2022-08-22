@FilterDefs({
    @FilterDef(
            name = "filterByDeleted",
            defaultCondition = "deleted = false"
    )
})

@org.hibernate.annotations.GenericGenerator(
        name = "uuidv2Generator",
        strategy = "uuid2"
)

@org.hibernate.annotations.GenericGenerator(
        name = "shortUUIDGenerator",
        strategy = "com.corvid.bes.model.ShortUUIDGenerator"
)

@org.hibernate.annotations.GenericGenerator(
        name = "base32UUIDGenerator",
        strategy = "com.corvid.bes.model.Base32UUIDGenerator"
)

package com.corvid.bes.model;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;