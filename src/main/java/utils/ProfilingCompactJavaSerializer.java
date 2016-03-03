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

import net.sf.ehcache.util.concurrent.LongAdder;
import org.ehcache.exceptions.SerializerException;
import org.ehcache.impl.serialization.CompactJavaSerializer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Ludovic Orban
 */
public class ProfilingCompactJavaSerializer extends CompactJavaSerializer {

  public final ConcurrentMap<String, LongAdder> reads = new ConcurrentHashMap<String, LongAdder>();
  public final ConcurrentMap<String, LongAdder> equals = new ConcurrentHashMap<String, LongAdder>();
  public final LongAdder equalsTrue = new LongAdder();
  public final LongAdder equalsFalse = new LongAdder();

  private static String getStackTrace() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    Exception exception = new Exception("Stack trace");
    exception.printStackTrace(pw);
    pw.close();
    return sw.toString();
  }

  public ProfilingCompactJavaSerializer(ClassLoader loader) {
    super(loader);
  }

  @Override
  public ByteBuffer serialize(Object object) throws SerializerException {
    return super.serialize(object);
  }

  @Override
  public Object read(ByteBuffer binary) throws ClassNotFoundException, SerializerException {
    String stackTrace = getStackTrace();
    LongAdder counter = new LongAdder();
    LongAdder existing = reads.putIfAbsent(stackTrace, counter);
    if (existing != null) {
      counter = existing;
    }
    counter.increment();

    return super.read(binary);
  }

  @Override
  public boolean equals(Object object, ByteBuffer binary) throws ClassNotFoundException, SerializerException {
    String stackTrace = getStackTrace();
    LongAdder counter = new LongAdder();
    LongAdder existing = equals.putIfAbsent(stackTrace, counter);
    if (existing != null) {
      counter = existing;
    }
    counter.increment();

    boolean equals = super.equals(object, binary);
    if (equals) {
      equalsTrue.increment();
    } else {
      equalsFalse.increment();
    }
    return equals;
  }

}
