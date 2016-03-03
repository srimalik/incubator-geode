/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gemstone.gemfire.cache.util;

/**
 * This class provides an implementation of the ObjectSizer interface. This
 * uses a helper class to provide the size of an object. The implementation uses
 * reflection to compute object size. This is to be used for testing purposes only
 * This implementation is slow and may cause throughput numbers to drop if used on
 * complex objects.
 *
 *
 * @deprecated use {@link ObjectSizer#DEFAULT} instead.
 */
public class ObjectSizerImpl implements ObjectSizer {

  public int sizeof( Object o ) {
    return ObjectSizer.DEFAULT.sizeof(o);
  }
}
