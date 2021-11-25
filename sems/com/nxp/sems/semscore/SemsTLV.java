/*
 * Copyright 2019, 2021 NXP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nxp.sems;

import java.io.ByteArrayOutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class SemsTLV {
  private int tag;
  private List<SemsTLV> nodes;
  private byte[] value;

  private SemsTLV(int tag, byte[] value, List<SemsTLV> nodes) {
    this.tag = tag;
    this.value = value;
    this.nodes = nodes;
  }

  private SemsTLV(int tag, byte[] value) {
    this.tag = tag;
    this.value = value;
  }

  public static byte[] make(int tag, byte[] value) {
    return new SemsTLV(tag, value).getTLV();
  }

  public boolean isConstructed() { return this.nodes != null; }

  public boolean isPrimitive() { return this.nodes != null; }

  public int getTag() { return this.tag; }

  public List<SemsTLV> getNodes() {
    if (nodes == null) {
      throw new RuntimeException("Bad call");
    }
    return this.nodes;
  }

  public int getLength() { return getValue().length; }

  public byte[] getValue() { return this.value; }

  public byte[] getTLV() { return createTLV(this.tag, this.value); }

  public static List<SemsTLV> parse(byte[] buffer) {
    return parse(ByteBuffer.wrap(buffer));
  }

  public static List<SemsTLV> parse(byte[] buffer, int[] asPrimitiveTags) {
    return parse(ByteBuffer.wrap(buffer), asPrimitiveTags);
  }

  private static List<SemsTLV> parse(ByteBuffer bb) { return parse(bb, null); }

  private static List<SemsTLV> parse(ByteBuffer bb, int[] asPrimitiveTags) {
    List<SemsTLV> list = new ArrayList<SemsTLV>();

    for (;;) {
      int tag;

      try {
        tag = bb.get() & 0xFF;
      } catch (BufferUnderflowException e) {
        break;
      }

      boolean isPrimitive = (tag & 0x20) == 0x00;

      if ((tag & 0x1F) == 0x1F) {
        /* 2-byte tag*/
        tag = (tag << 8) + (bb.get() & 0xFF);
      }

      if (!isPrimitive && (asPrimitiveTags != null)) {
        for (int asPrimitiveTag : asPrimitiveTags) {
          if (tag == asPrimitiveTag) {
            isPrimitive = true;
            break;
          }
        }
      }

      int length = bb.get() & 0xFF;
      if (length <= 0x7F) {
        /* 1-byte length*/
      } else if (length == 0x81) {
        length = bb.get() & 0xFF;
      } else if (length == 0x82) {
        int length1 = bb.get() & 0xFF;
        int length2 = bb.get() & 0xFF;
        length = (length1 << 8) + length2;
      } else if (length == 0x83) {
        int length1 = bb.get() & 0xFF;
        int length2 = bb.get() & 0xFF;
        int length3 = bb.get() & 0xFF;
        length = (length1 << 16) + (length2 << 8) + length3;
      } else if (length == 0x84) {
        int length1 = bb.get() & 0xFF;
        int length2 = bb.get() & 0xFF;
        int length3 = bb.get() & 0xFF;
        int length4 = bb.get() & 0xFF;
        length = (length1 << 24) + (length2 << 16) + (length3 << 8) + length4;
      } else {
        throw new RuntimeException("Bad length field");
      }

      byte[] value = new byte[length];
      bb.get(value, 0, value.length);

      if (isPrimitive) {
        list.add(new SemsTLV(tag, value));
      } else {
        list.add(new SemsTLV(tag, value, SemsTLV.parse(value)));
      }
    }

    return list;
  }

  public static SemsTLV find(List<SemsTLV> nodes, int tag) {
    for (SemsTLV tlv : nodes) {
      if (tlv.getTag() == tag) {
        return tlv;
      }
    }

    return null;
  }

  public static byte[] createTLV(int tag, byte[] value) {
    if (value == null) {
      return new byte[0];
    } else {
      byte[] t, l;

      if ((tag & 0xFF00) == 0x0000) {
        t = new byte[] {(byte)tag};
      } else {
        t = new byte[] {(byte)(tag >> 8), (byte)tag};
      }

      if (value.length < 128) {
        l = new byte[] {(byte)value.length};
      } else if (value.length < 256) {
        l = new byte[] {(byte)(0x81), (byte)value.length};
      } else if (value.length < 65536) {
        l = new byte[] {(byte)(0x82), (byte)(value.length >> 8),
                        (byte)value.length};
      } else if (value.length < (256 * 65536)) {
        l = new byte[] {(byte)(0x83), (byte)(value.length >> 16),
                        (byte)(value.length >> 8), (byte)value.length};
      } else {
        l = new byte[] {(byte)(0x84), (byte)(value.length >> 24),
                        (byte)(value.length >> 16), (byte)(value.length >> 8),
                        (byte)value.length};
      }

      return SemsUtil.append(SemsUtil.append(t, l), value);
    }
  }

  public static byte[] make(List<SemsTLV> tlvs) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      for (SemsTLV tlv : tlvs) {
        out.write(tlv.getTLV());
      }

      return out.toByteArray();
    } catch (Exception e) {
      return null;
    }
  }

  public static int getTagOffset(byte[] buff, int tagReq) {
    int sOffset = 0;
    int nextOffset = 0;
    ByteBuffer bb = ByteBuffer.wrap(buff);
    for (;;) {
      int tag;

      try {
        tag = bb.get() & 0xFF;
        nextOffset++;
      } catch (BufferUnderflowException e) {
        Log.e("SemsTLV", "BufferUnderFlowException occured");
        break;
      }

      boolean isPrimitive = (tag & 0x20) == 0x00;

      if ((tag & 0x1F) == 0x1F) {
        /* 2-byte tag*/
        tag = (tag << 8) + (bb.get() & 0xFF);
        nextOffset++;
      }
      if (tag == tagReq) {
        return sOffset;
      }

      int length = bb.get() & 0xFF;
      if (length <= 0x7F) {
        /* 1-byte length*/
        nextOffset++;
      } else if (length == 0x81) {
        length = bb.get() & 0xFF;
        nextOffset += 2;
      } else if (length == 0x82) {
        int length1 = bb.get() & 0xFF;
        int length2 = bb.get() & 0xFF;
        length = (length1 << 8) + length2;
        nextOffset += 3;
      } else if (length == 0x83) {
        int length1 = bb.get() & 0xFF;
        int length2 = bb.get() & 0xFF;
        int length3 = bb.get() & 0xFF;
        length = (length1 << 16) + (length2 << 8) + length3;
        nextOffset += 4;
      } else if (length == 0x84) {
        int length1 = bb.get() & 0xFF;
        int length2 = bb.get() & 0xFF;
        int length3 = bb.get() & 0xFF;
        int length4 = bb.get() & 0xFF;
        length = (length1 << 24) + (length2 << 16) + (length3 << 8) + length4;
        nextOffset += 5;
      } else {
        throw new RuntimeException("Bad length field");
      }

      if (isPrimitive) {
        byte[] value = new byte[length];
        nextOffset += length;
        bb.get(value, 0, value.length);
      } else {
        /* In case of constructed TLV only tag, length counted, value skipped
        for offset calculation*/
      }
      // Update offset pointing to next tag
      sOffset = nextOffset;
    }
    return -1;
  }
}
