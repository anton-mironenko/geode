/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.protocol.protobuf;

import org.apache.geode.pdx.JSONFormatter;
import org.apache.geode.pdx.PdxInstance;
import org.apache.geode.serialization.SerializationType;
import org.apache.geode.serialization.exception.UnsupportedEncodingTypeException;

import java.util.HashMap;

/**
 * This class maps protobuf specific encoding types and the corresponding serialization types.
 */
public abstract class EncodingTypeTranslator {
  static final HashMap<Class, BasicTypes.EncodingType> typeToEncodingMap = intializeTypeMap();

  private static HashMap<Class, BasicTypes.EncodingType> intializeTypeMap() {
    HashMap<Class, BasicTypes.EncodingType> result = new HashMap<>();
    result.put(Integer.class, BasicTypes.EncodingType.INT);
    result.put(Byte.class, BasicTypes.EncodingType.BYTE);
    result.put(Long.class, BasicTypes.EncodingType.LONG);
    result.put(Float.class, BasicTypes.EncodingType.FLOAT);
    result.put(Short.class, BasicTypes.EncodingType.SHORT);
    result.put(byte[].class, BasicTypes.EncodingType.BINARY);
    result.put(Double.class, BasicTypes.EncodingType.DOUBLE);
    result.put(String.class, BasicTypes.EncodingType.STRING);
    result.put(Boolean.class, BasicTypes.EncodingType.BOOLEAN);
    return result;
  }

  public static SerializationType getSerializationTypeForEncodingType(
      BasicTypes.EncodingType encodingType) throws UnsupportedEncodingTypeException {
    switch (encodingType) {
      case INT:
        return SerializationType.INT;
      case BYTE:
        return SerializationType.BYTE;
      case JSON:
        return SerializationType.JSON;
      case LONG:
        return SerializationType.LONG;
      case FLOAT:
        return SerializationType.FLOAT;
      case SHORT:
        return SerializationType.SHORT;
      case BINARY:
        return SerializationType.BINARY;
      case DOUBLE:
        return SerializationType.DOUBLE;
      case STRING:
        return SerializationType.STRING;
      case BOOLEAN:
        return SerializationType.BOOLEAN;
      default:
        throw new UnsupportedEncodingTypeException(
            "No serialization type found for protobuf encoding type: " + encodingType);
    }
  }

  public static BasicTypes.EncodingType getEncodingTypeForObject(Object resultValue)
      throws UnsupportedEncodingTypeException {
    if (resultValue instanceof PdxInstance) {
      String pdxClassName = ((PdxInstance) resultValue).getClassName();
      if (pdxClassName.equals(JSONFormatter.JSON_CLASSNAME)) {
        return BasicTypes.EncodingType.JSON;
      }
    }

    BasicTypes.EncodingType encodingType = typeToEncodingMap.get(resultValue.getClass());
    if (encodingType == null) {
      throw new UnsupportedEncodingTypeException(
          "We cannot translate: " + resultValue.getClass() + " into a specific Protobuf Encoding");
    } else {
      return encodingType;
    }
  }
}
