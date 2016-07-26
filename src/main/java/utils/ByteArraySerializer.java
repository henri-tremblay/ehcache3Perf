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

import org.ehcache.spi.serialization.Serializer;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author Ludovic Orban
 */
public class ByteArraySerializer implements Serializer {

  public ByteArraySerializer() {
  }

  public ByteArraySerializer(ClassLoader classLoader) {
  }


  @Override
  public ByteBuffer serialize(Object o) {
    byte[] array = (byte[]) o;
    ByteBuffer buffer = ByteBuffer.wrap(array);
    return buffer;
  }

  @Override
  public Object read(ByteBuffer byteBuffer) throws ClassNotFoundException {
    byte[] array = new byte[byteBuffer.remaining()];
    byteBuffer.get(array);
    return array;
  }

  @Override
  public boolean equals(Object o, ByteBuffer byteBuffer) throws ClassNotFoundException {
    byte[] array = (byte[]) o;
    return Arrays.equals(array, (byte[]) read(byteBuffer));
  }
}
