package com.corvid.bes.model;

import java.io.Serializable;
import java.util.UUID;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import com.github.f4b6a3.uuid.codec.base.Base32Codec;

public class Base32UUIDGenerator implements IdentifierGenerator {

   @Override
   public Serializable generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object o)
    throws HibernateException {
      return Base32Codec.INSTANCE.encode(UUID.randomUUID());
   }
}