package com.corvid.bes.model;

import java.io.Serializable;
import java.util.UUID;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class ShortUUIDGenerator implements IdentifierGenerator {

   @Override
   public Serializable generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object o)
    throws HibernateException {
      return UUID.randomUUID().toString().replace("-", "");
   }
}

//Base32Codec.INSTANCE.encode(uuid)