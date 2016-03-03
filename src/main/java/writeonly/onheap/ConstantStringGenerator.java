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
package writeonly.onheap;

import io.rainfall.ObjectGenerator;

/**
 * @author Ludovic Orban
 */
public class ConstantStringGenerator implements ObjectGenerator<String> {
  private final String string;
  private int length;

  public ConstantStringGenerator(int length) {
    this.length = length;
    if (length <= 0) {
      throw new IllegalStateException("Can not generate a String with a length less or equal to 0");
    } else {
      StringBuffer outputBuffer = new StringBuffer(length);

      for (int i = 0; i < length; ++i) {
        outputBuffer.append("0");
      }

      this.string = outputBuffer.toString();
    }
  }

  public String generate(Long seed) {
    return string;
  }

  public String getDescription() {
    return "String (length = " + this.length + ")";
  }

  public static ObjectGenerator<String> fixedLength(int length) {
    return new ConstantStringGenerator(length);
  }
}
