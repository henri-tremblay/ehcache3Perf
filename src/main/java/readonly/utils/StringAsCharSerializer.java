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
package readonly.utils;

import org.ehcache.exceptions.SerializerException;
import org.ehcache.spi.serialization.Serializer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * @author Ludovic Orban
 */
public class StringAsCharSerializer implements Serializer<String> {
  @Override
  public ByteBuffer serialize(String object) throws SerializerException {
    char[] chars = object.toCharArray();
    ByteBuffer byteBuffer = ByteBuffer.allocate(chars.length * 2);
    CharBuffer charBuffer = byteBuffer.asCharBuffer();
    charBuffer.put(chars);
    return byteBuffer;
  }

  @Override
  public String read(ByteBuffer binary) throws ClassNotFoundException, SerializerException {
    char[] chars = new char[binary.remaining() / 2];
    binary.asCharBuffer().get(chars);
    return new String(chars);
  }

  @Override
  public boolean equals(String object, ByteBuffer binary) throws ClassNotFoundException, SerializerException {
    return object.equals(read(binary));
  }
}
