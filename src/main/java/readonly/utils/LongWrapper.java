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

import java.io.Serializable;

/**
 * @author Ludovic Orban
 */
public class LongWrapper implements Serializable {
  private final Long delegate;

  public LongWrapper(Long delegate) {
    this.delegate = delegate;
  }

  public Long getDelegate() {
    return delegate;
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LongWrapper) {
      return ((LongWrapper) obj).getDelegate().equals(delegate);
    }
    return false;
  }
}
