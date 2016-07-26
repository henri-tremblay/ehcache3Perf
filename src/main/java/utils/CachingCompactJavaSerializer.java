/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package utils;

import org.ehcache.impl.serialization.CompactJavaSerializer;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class CachingCompactJavaSerializer extends CompactJavaSerializer {

  private final ThreadLocal<Map<Long, Object>> cache = new ThreadLocal<Map<Long, Object>>() {
    @Override
    protected Map<Long, Object> initialValue() {
      return new HashMap<Long, Object>();
    }
  };

  public CachingCompactJavaSerializer(ClassLoader loader) {
    super(loader);
  }

  @Override
  public ByteBuffer serialize(Object object) {
    return super.serialize(object);
  }

  @Override
  public Object read(ByteBuffer binary) throws ClassNotFoundException {
    DirectBuffer directBuffer = null;
    if (binary.isDirect()) {
      directBuffer = (DirectBuffer) binary;
      Object o = cache.get().get(directBuffer.address());
      if (o != null) {
        return o;
      }
    }

    Object read = super.read(binary);
    if (directBuffer != null) {
      cache.get().clear();
      cache.get().put(directBuffer.address(), read);
    }
    return read;
  }

  @Override
  public boolean equals(Object object, ByteBuffer binary) throws ClassNotFoundException {
    return object.equals(read(binary));
  }
}
