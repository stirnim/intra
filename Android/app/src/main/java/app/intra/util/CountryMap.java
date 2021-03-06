/*
Copyright 2018 Jigsaw Operations LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package app.intra.util;

import android.content.res.AssetManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Map IP addresses to country codes, using a fixed-width sorted database.
 * Lookups are performed using a binary search.
 * The database requires about 5 MB of RAM, 2 MB compressed in the APK.  A carefully designed
 * tree-based representation could probably save a factor of 4.
 * Note that this class is not used by the service, so it should only contribute to RAM usage when
 * the UI is visible.
 */
public class CountryMap {

  // Number of bytes used to store each country string.
  private static final int COUNTRY_SIZE = 2;

  private final byte[] v4db;
  private final byte[] v6db;

  public CountryMap(AssetManager assetManager) throws IOException {
    v4db = read(assetManager.open("dbip.v4"));
    v6db = read(assetManager.open("dbip.v6"));
  }

  private static byte[] read(InputStream input) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int n;
    byte[] temp = new byte[4096];
    while ((n = input.read(temp, 0, temp.length)) != -1) {
      buffer.write(temp, 0, n);
    }
    return buffer.toByteArray();
  }

  private static boolean lessEqual(byte[] a, byte[] b) {
    for (int i = 0; i < a.length; ++i) {
      int ai = a[i] & 0xFF;
      int bi = b[i] & 0xFF;

      if (ai < bi) {
        return true;
      }
      if (ai > bi) {
        return false;
      }
    }
    return true;
  }

  public String getCountryCode(InetAddress address) throws IOException {
    byte[] key = address.getAddress();
    byte[] db = key.length == 4 ? v4db : v6db;
    int recordSize = key.length + COUNTRY_SIZE;
    int low = 0;
    int high = db.length / recordSize;
    while (high - low > 1) {
      int mid = (low + high) / 2;
      int position = mid * recordSize;
      byte[] v = Arrays.copyOfRange(db, position, position + key.length);
      if (lessEqual(v, key)) {
        low = mid;
      } else {
        high = mid;
      }
    }
    int position = low * recordSize + key.length;
    byte[] countryCode = Arrays.copyOfRange(db, position, position + COUNTRY_SIZE);
    return new String(countryCode, Charset.forName("UTF-8"));
  }
}
