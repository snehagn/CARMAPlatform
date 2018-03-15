/*
 * Copyright (C) 2018 LEIDOS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package gov.dot.fhwa.saxton.carma.message.helper;

/**
 * This class has three functionalities. It can convert a string type into
 * a byte array and set the byte array as a field based on input field parameter.
 * It will handle both a field with required length or a field with dynamic
 * length. It can also convert a long type timestamp into a byte array and
 * set the byte array as a filed based on the input field parameter. This class
 * will be used mainly by different Mobility helper class. Its third functionality
 * is to read all data between '[' and ']' from a byte[] and return as a String. 
 */
public class StringConverterHelper {
    
    public static final String DYNAMIC_STRING_DEFAULT = "[]";
    public static final int TIMESTAMP_LENGTH = Long.toString(Long.MAX_VALUE).length();
    
    public static void setDynamicLengthString(String inputString, byte[] field, int maxLength) {
        char[] tmp;
        if(inputString.length() <= maxLength) {
            inputString = "[" + inputString + "]";
            tmp = inputString.toCharArray();   
        } else {
            tmp = DYNAMIC_STRING_DEFAULT.toCharArray();
        }
        field = new byte[tmp.length];
        for(int i = 0; i < tmp.length; i++) {
            field[i] = (byte) tmp[i];
        }
    }
    
    public static void setFixedLengthString(String inputString, byte[] field, int length, String defaultString) {
        char[] tmp;
        if(inputString.length() == length) {
            tmp = inputString.toCharArray();
        } else {
            tmp = defaultString.toCharArray();
        }
        field = new byte[length];
        for(int i = 0; i < tmp.length; i++) {
            field[i] = (byte) tmp[i];
        }
    }
    
    public static void setTimestamp(long time, byte[] field) {
        StringBuffer number = new StringBuffer(Long.toString(time));
        int numberOfZeroNeeded = TIMESTAMP_LENGTH - number.length();
        for(int i = 0; i < numberOfZeroNeeded; i++) {
            number.insert(0, '0');
        }
        char[] tmp = number.toString().toCharArray();
        field = new byte[tmp.length];
        for(int i = 0; i < tmp.length; i++) {
            field[i] = (byte) tmp[i];
        }
    }
    
    public static String readDynamicLengthString(byte[] input) {
        StringBuffer buffer = new StringBuffer();
        boolean startRead = false;
        for(byte ch : input) {
            // Because of the defect on asn1c compiler, we force to only put string between '[' and ']'
            if(startRead) {
                if(ch == 93) {
                    startRead = false;
                    break;
                } else {
                    buffer.append((char) ch);
                }
            } else {
                if(ch == 91) {
                    startRead = true;
                }
            }
        }
        return buffer.toString();
    }
}
