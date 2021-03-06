/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.tools.lint.checks;

import com.android.tools.lint.checks.infrastructure.TestFile;
import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("SpellCheckingInspection")
public class SecureRandomGeneratorDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new SecureRandomGeneratorDetector();
    }

    public void testWithoutWorkaround() {
        String expected =
                ""
                        + "src/test/pkg/PrngCalls.java:13: Warning: Potentially insecure random numbers on Android 4.3 and older. Read https://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html for more info. [TrulyRandom]\n"
                        + "        KeyGenerator generator = KeyGenerator.getInstance(\"AES\", \"BC\");\n"
                        + "                                              ~~~~~~~~~~~\n"
                        + "0 errors, 1 warnings\n";
        lint().files(classpath(), manifest().minSdk(10), mPrngCalls).run().expect(expected);
    }

    public void testWithWorkaround() {
        lint().files(
                        classpath(),
                        manifest().minSdk(10),
                        mPrngCalls,
                        base64gzip(
                                "bin/classes/test/pkg/PrngWorkaround$LinuxPRNGSecureRandom.class",
                                ""
                                        + "H4sIAAAAAAAAAJVWWXMUVRT+bjJJTyZNCIEQsgADBJzJwkDYxLBmw8GZBAgE"
                                        + "Y1TszHSSJpPpcaYHElwRd/0Flu9UWT6waGIZS6rggYJS0fLB8gVLSy0f1Ad8"
                                        + "QtH43Z41C1b5MHNu33vOuef7zndv981/Pv4UQAveVlAg0GzpCcsXGx32HYpH"
                                        + "h4+b8VEtbiaj4fqAEU2OHzrSfaBXDyXj+hEtGjbHFDgEVp7UTmm+hJw2rAlf"
                                        + "/npvzBBQjx3Z393REzzR5Q90CpQFbH/D9HUZEb1VoCgRMEOjAhWphYgWHfb1"
                                        + "DJ7UQxYXXYljcTuVPypQnQ3t0CzNH40lrV4rrmtjdCzNOPYkLYHlWU8+5rsp"
                                        + "Y726HtbDAuIxAeeuUMSIGtYegUKPt0/A0W6GdQVLWXV+lU5UcsYX1k/5kqld"
                                        + "XKhAlYoVqBYo3pXOUenJg8Atjehwq7evBAK1KopQrGClQPlclC7UYbVMVS49"
                                        + "16hQ4BRYTML17uTYoB4/qg1GdMmPGdIifVrckM/2pAuFWM2qrREjIeAL/K/m"
                                        + "kY1FenSY2/TqlmSFrfAMtHn7XCyjQUUjmkjksG7liM0yKbDC412YYxc2YpMC"
                                        + "n8CyhdZVbIaHO52mWHTpu0XFVpRzZiiSTIxICrarWIQyBQ+Stzw+0wLrHA/p"
                                        + "McswowoeEqiaS3hb0oiE9bgTu1hjl8bmhd2W6R4zxt0JYnQbUT65sBNV8m+v"
                                        + "in3YzxZqsZgeJQPNnvkq9M7ranqTVpmiXUUHqGunZaYWCdwzP4S+O3BAtvlh"
                                        + "sruAUPJmjo7EzdOyv1SPgoMCSzNM+nuy8MnY4AQbLlAw0Eb9mlL3gjqpDCzg"
                                        + "LZvda2mh0aAWs6WjIKCgN5N69rZUX0oY3fq41SY3caEPjyo4TsLvozEV/ZAn"
                                        + "SguF9ESifhNV6fEOtEktPa7iAXjk6EkVJ/AUS8ypKu8Us915opp7xF0YRFhB"
                                        + "KNPz+S4qdCmtEo7DXclIZMIJ2YycCOSCeyhujrlJmkEGK1IwD+hRPa5ZeuYQ"
                                        + "+NOFR1QclRkdCeMMSRF+OaSPFOnTKkrgUpDIq0deFXn1uBBHUnb8lMASz+xr"
                                        + "T56yQYzL1Ql5mHI9y4Ps7XPiGTYjh8CkSt1OPMfbyD1kxm1EFI/U4QtSyi/O"
                                        + "UXDmErqvgiWUl1SUQlXwMgvJLzL/2LpwDkknXstsLI+vrXVXr5mMh3TpTzHN"
                                        + "1sRGmY0R/ij5bY9oiYSUa+WCtxHWsJJCvomKUCPvSo5q5E1I65JM25Zl0hbI"
                                        + "64FWri5GOeOW8GmvHQ1UTaOiv2LZFJZfRs006vqnsOoy3BftwLX8L7Md32Oq"
                                        + "97Ec72KdvQKmWZFN5qMVspiGKdTnQovtyVSImnLAemygdUiJp4JFOWcdnNt/"
                                        + "Ce7+7qtM4Q02XzuP4uZrV5oaJ9HcNImWBseH2HYeq4PT2NE/jZ39Fa1T2H0J"
                                        + "NZPYM4m2pil0XbnISosJtpzJyvnCKbcBrkE9/FyRBW0kJcANet3k6AaW4jOi"
                                        + "+JxvlC/odYvFfQkvvsIu2n342i58K8tjcenC5egRBFh6KeeDhFXALEvSo/Wo"
                                        + "RTd6ODpEzx1wzLAIhwKh4DD5r5ODIzNcLkzNKawI0qcg7cM/fy3ZOJplJ5hm"
                                        + "p6fhA2x7B66Gj3BsEgNZrp7IccUJyZcmJ9ZyIsfV0AJcFWEZq13F1Kvgtu16"
                                        + "NLGTspubbVvGz5yWLHfbySzwDWF8y5XbjP4O1bRr8D0jf0ADbQt+xBb8ROQ/"
                                        + "4yBtN37J47Any2FPmsNqzo9QqAXMsiQ92jKLw24Ul81Qtv9J4qAkcVHGh89y"
                                        + "KuM1l1oDJ9OqPSxz0JbWfuIMNlBlo00X5sj+Vwb8xvP0uw3DnXLPwijFGKJM"
                                        + "5mBZJgFxM3lhp9PfJjjpH5S9Cly9hNh1tE5jkC2J97MbU7CmcPoyYudRn2vV"
                                        + "mWyrKp6dxPOZfjG68dqFxmtXmFJlIevYl2L+DnKDg9xa2nyN3+HqH/S8S3H9"
                                        + "yavhLiP+osbvsbd/U9v30CFk5B0b2HL7RAfTwOqZP9eBKhTO0ClFo02+391J"
                                        + "brnMT5401ltprF0ZrGevY+s0zqVwvnIZZ+8H8tVZIM/mgazmbhJkG9qZuR2d"
                                        + "tk2B3MCeQPDeE8VQhRN1ooQfiy40iFLspN0tytDO9Ry4rjS4lfPB1S0I7nVb"
                                        + "Cm/gTfsqlfdbH95Cyb+UCGS4AQwAAA=="),
                        base64gzip(
                                "bin/classes/test/pkg/PrngWorkaround$LinuxPRNGSecureRandomProvider.class",
                                ""
                                        + "H4sIAAAAAAAAAJ1SwW7TQBB906S1YwyhbtNCoa2hBZJAa4GEQAqqVBUBkaIQ"
                                        + "NSg9cdjY29Rtso7W6wDfAJyAD+FSJA58AB+FWLsFilKQwJZmZ9/svJk3u1+/"
                                        + "ff4C4A5uGZgg3FU8Vt7woOe1pOjtRPKAySgRwWojFMnL1nbzcZv7ieTbTATR"
                                        + "oCWjURhwaSBPmNtnI+bFaThUr7wfMcLUg1CEaoOQK1c6hPxWFHATBULhJ6mJ"
                                        + "M4SNTTcD1uIh98Pd0HdlVsUVyaDLpTs8ZnTVHlNuEvPY9QI+8pKjYxZyOGtj"
                                        + "EucI5XIj66bPRM9rKxmKXu3hOFTpmDhPKJ3UtN5+snk77cmAQ1j7p3lYmMWc"
                                        + "gRKh+KvWVp/FsY15XCAYPa6abMAJs+XKeD8WCAs2LuGyHtYwUYT7J4U87e5z"
                                        + "X9XGkco4ZGKJsHKqMLc+GPb5gAvFg7ow4RLMdrSrXjCpGytqZbyZTfwZ6/Y1"
                                        + "4jQin/U7TIbp/hjMq70wJtxr/NeDqRGsdpRInz8KU7aZ35PXUzkEuy4El9n8"
                                        + "eGygQpj/QzV9h6fWIyz+tQ+4euI5pB/pfxJT2hp691z7pNflqmMWHesT7KpT"
                                        + "dKYPMXOIiztVZ9FZTp2P+sgErmi7oFOB15rsDfJ4iwLeoYT3WMIHXNUR+4gO"
                                        + "K1jNcq5l9jpu6NXR3jTKqOos/XZxE9Z3qzGd7ZcDAAA="),
                        base64gzip(
                                "bin/classes/test/pkg/PrngWorkaround.class",
                                ""
                                        + "H4sIAAAAAAAAAJVXCXhU1RX+L0l4k8kDwrAHkAEChpBFAlYkCGaFIZPFJAQD"
                                        + "2PRl5iV5MHlvePMmJFaLWmu1tdS22lZrtda1tbWA7ZBKFdtarXbf98Xu+2pr"
                                        + "F7T975slkzDBFr7Mfffec8695z//Offe515+7AkANWKBghkCixw95lRHDw5W"
                                        + "d9jm4B7LPqjZVtwMK8gXKD6gjWjVEc0crG7vP6CHHMr3NHV2Bdrb+hraG5v6"
                                        + "djUFg7199U11bQIiIDCrwTJjjmY6PVokrudxnWKBpdOo9LV21kgRn8Dq+t2B"
                                        + "YGNfc6BtR1NnR2egrbuvrq2xr7GpJ9DQ1NfV1BmoCwrM2Fcv4Nkaihim4WwT"
                                        + "yCtb1yOQ32CFdS8E5qrwYZ7AqkHdqY8bkXCzYQ7qdtQ2TKfODDfqI0ZI79Jt"
                                        + "Q4tQq2zdvvpCai1QUQRVYE7QMPW2+HC/bndr/RFdwBe0QlqkR6MC+6nBmVvd"
                                        + "xb3IQ4mKxZhNU86QERNYEpwGylqBAi0ajYzJTa5Q4ZdKc9yR9qhudnUFm41R"
                                        + "ObdKxWo5t8yQIEYi3FF8tKOzbUeXHorbeqdmhq1hAW/TaEiPOgahVrCW+E5E"
                                        + "yRU0nLGMRCHKUK5gncBiatuWEa62YtUuOqWpsKhYj5kCSldjSx+B96BSoMay"
                                        + "B6u0qBYa0quGNHvYMseqRk3dqYra1ogR1u2qA7GYXtWmOcaI3mCPRR3Li2ps"
                                        + "UHABXZvYT0NEi8VU1GAjFxiw7DZtmBiuKQtmbdlhfAZr1wWnaNV6cKFAYafk"
                                        + "QUzXSUhVLrFZxcXYwgkGuVV3hqywwKU57O07y172CrY+ECGdq5MWaiX4W1Vc"
                                        + "ItmjDuqmbmsOmaKHvdiOOgWXEr7plFXUo4G8MMwR6yCduzh7M8msmbSZ1NC6"
                                        + "s4c8aBKY7TocsbRw34AR0RXsmJSGSe8KEUCLgl0Un5gKWuagiiBaycju3g7a"
                                        + "mpsD03Z6WB3WR6rjtksnLy5Dl4JOSk8IB0xHZ+ao6MZuBm5EJnP7gMDCskD2"
                                        + "vlNitdLGHhWXo5fpaaSS303PgIJ9AvNcFcOqDrRniKngChaTqY65xNRtD/oE"
                                        + "Nuw29dEogdHDftPNS7814O8fY5b5bV0L+wdsa9jv5ohfJskWvxf7ocmk7Gda"
                                        + "5+JYj5RgxHTQmZnMQN0kfZZN9mrSTmqlxpAKAwfom2MlJwXml52tQtm90Dxg"
                                        + "ii5t1hi8sN+x/JK7/lSeu/v0Yg0sucuoQHmOXWaNdA/Z1mFZd7h1BXYaSXcu"
                                        + "gyRzwQWlk5iwChP4BcEcYqxCs7ocLXSwVYu6tcyDUYpml5aqrp11G5JbvBJX"
                                        + "KXgtI+5aiqXKSqa+qLgar3NzxelIVQRWwMpcmKe4nzGRlq9VcI3AhdPUzNKc"
                                        + "tS+tK6vvdSpej+sZFO7BJTfRKTu7ikjRG1S8ETcy4vqhuBaJTSFHOiH3enEE"
                                        + "JdL1N6u4GW9h3rEO63bGwzpHcmU6dwKS7G8VKMmBWGr7XhyVCxzF21W8A7cK"
                                        + "FGXhR8309s827sE7Ca+pH/Zn2ytb5+9nQMmv/jH/YZv5709rJJPh3ZLqt08J"
                                        + "TI4CNJXyaySN7yS2E4Q4irtU3I33JTcdcE/5ELl2wfS1PCcGdOX9pFXasN+0"
                                        + "HD+FjUiSkvcJbJ9EyazFylaltVady3EFDwisnbyDNqsrHhqqiwxa7A4NZ+XO"
                                        + "glgOfsnTfFresrySqhvIhXO5mZSqEZhKmOm2wnx4SMFHBJanq2U9s7rOtrWx"
                                        + "9rgTjTsEV9fIoUdQouBE+kSiXKPmaJNFjuOjssB8jG6klqdYtohbCk/i4wrG"
                                        + "Jx8vYzFHH1bxGE7xQOCObd10uo1hvdWIROQlhzV9l7T/uIoncJql5zC90OXR"
                                        + "I69Uu5JmP6niU9KAx9RMS2p78RSeVvAZ3qmybiFENKTL68Ez8uAoGB7rMMLS"
                                        + "9rMqnsPnqO7a5iEjTQek6afwBRVfTEnvTkp/WcVX8FWOuNJsy/bVS9nj+LqK"
                                        + "b8jrVEEoYsV0Cdy3VHxbnvFFjpUB14Pv8iCaqNfp498t3LxsyaY+PjAgM3RF"
                                        + "8JyxkSV2QpwzzOngdFGqzdTpyQejHCfps2+ryVupgp8wVlNvcR78jIUteUOW"
                                        + "96NfqPglfpUsi82GHglPU5ZzXIZccWb/b/A7Bb+ddDpPElHxe/yBVOAKTLRX"
                                        + "KC2pId4ijEHTsuWVaj9KCvE8/qLir3iBwci69ctLd45jdT/+LivZi6/sy9RK"
                                        + "JvBPFT/GAQ/+TSLs7m6u3OxFM15S8TL+k0RJhjIm7za5Dq96RYh0TXdnOuOm"
                                        + "Q0ZnwuUReawErmG/boasMPXcohaLR6OWzcuLVwCaInjFLk2He7eZmW1K6WSV"
                                        + "pJm2HotHHPmimM4xojYw8bihSiz1rFkb/F/WoL5HC8nkK+V93dtlxe2Q3mzI"
                                        + "9828yedwlTTHgz5gMivcE1VipaSeDorg263y/zrCJetzjy8/55GPDYylfC3y"
                                        + "3YUCzGRPYW8GPOwXZvW97PNBx+9Czs3CbM7MYa+arfynjKP4Ucw/7gov5O9s"
                                        + "1+xK/vppZhkWuTPyb/EkZaleUH4SSyZUZ7qDpa6KmhTAUpqQay/PUkZ65fPG"
                                        + "sXLqyhX8raSpqszKXvlE5Fcpv/NpZI00I6pSy912AucXF9+DItn67kP+cV/F"
                                        + "OKp8m/IfR3Vvnu9VXQlcJNjJ680bRy172/YkJRoLkhLNXb35J7DTlStw5Xxt"
                                        + "HJqbj3F0SPkncVkCPbUlHLkb55/C3t5T2N/re/VJvKYkgVACgydx8HTLKazp"
                                        + "9UUIiXn6uHx/40b+P0R3pHOdmM/fixiDzSjGFs5u5/ildKoOG9GEC9HImWY+"
                                        + "WHagn98GdiLKd42DXbxctuBattezfyNacRPacCs6XIAWEhgDKxAjHOBMETXi"
                                        + "/BrhXxHyi/EiRhQcKiKKq3OgeBdDlM+RhyfQOzyOsZbyp7Go/In8ezDXd6Q8"
                                        + "ryaBaxN4wx2YcwpHek/ipvxxvGnPKRzl9y1B35H1CbwtLbJSwuDicxvxycy8"
                                        + "y0XpjtO+94zjva0PYk6bC9c9lRIu35GKnBbupYWKqRYktkMwiez9KWxfR/+B"
                                        + "fcT2CjItRDf76VwY81gkl2CAHBwkoQdwPvU2UrMWFh+3UaJsEdVDRNPGHiKo"
                                        + "sQ2zP4RR/o6xdyWxv4r34KtxA9ubudIt7N2Oa3Av2wfY/yCuc+OwiUDOwZ14"
                                        + "EA+xV0vrH+Cc4Gp5eJhfM2ijKvVlu1H6EGdklJZCKTyD5Qo+XHQGmxQc3ang"
                                        + "/jMo4lcFo8ZXeCpxnmGkZD4ETuERAn+MhDveS7o9Glw/jkQCn2DzZLL5dAKf"
                                        + "ZfN52ZzA/AS+xEB8rTyBbx5L0vQ7Lk3dBNrKJfamkKyRlUIsgUcsRbEowVyx"
                                        + "DIvFcqwQ52GtWIFK4ccmsRKbxSpcIlZjm0hmu5/6HnL4e/g+e3OZxD/AD2l9"
                                        + "m+vpj1J8ZMJerXAtrzyCUk5dTpfkLny+530/TeDnIoFfP4nmYy3imDs+m1Lp"
                                        + "HHILhNiAOaKGu9uYqTPFtPHHDOu5SjsZz1V8Geiepaak+RZJq5P4U8sJ/Dm4"
                                        + "/mkUlhOXv+0Zxz9aK2SvQvYI06DvXwmcOdbGW1vvPDHjpMiXWJWyMFWJgtRu"
                                        + "yrkCxGZidTFUsYU7qsVCcQlKxDacJ7ajVNRTugHVotHdaTl34EGNUITH3XeV"
                                        + "KMQLtFoCv/DyawYrI9ESRSleLEDeS1ioYL+C5jOYxd+AIgqKPULNuFWaKuJM"
                                        + "x9pjk8uwWDRRucUs93e2kDolNF+G50UxCsVcCDGPKXOE7Xx4/wvMVaJiDBUA"
                                        + "AA=="))
                .run()
                .expectClean();
    }

    public void testCipherInit() {
        String expected =
                ""
                        + "src/test/pkg/CipherTest1.java:14: Warning: Potentially insecure random numbers on Android 4.3 and older. Read https://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html for more info. [TrulyRandom]\n"
                        + "        cipher.init(Cipher.WRAP_MODE, key); // FLAG\n"
                        + "               ~~~~\n"
                        + "0 errors, 1 warnings\n";
        lint().files(
                        classpath(),
                        manifest().minSdk(16),
                        compiled(
                                "bin/classes",
                                java(
                                        ""
                                                + "package test.pkg;\n"
                                                + "\n"
                                                + "import java.security.InvalidKeyException;\n"
                                                + "import java.security.Key;\n"
                                                + "import java.security.NoSuchAlgorithmException;\n"
                                                + "import java.security.SecureRandom;\n"
                                                + "\n"
                                                + "import javax.crypto.Cipher;\n"
                                                + "import javax.crypto.NoSuchPaddingException;\n"
                                                + "\n"
                                                + "@SuppressWarnings(\"all\")\n"
                                                + "public class CipherTest1 {\n"
                                                + "    public void test1(Cipher cipher, Key key) throws InvalidKeyException {\n"
                                                + "        cipher.init(Cipher.WRAP_MODE, key); // FLAG\n"
                                                + "    }\n"
                                                + "\n"
                                                + "    public void test2(Cipher cipher, Key key, SecureRandom random) throws InvalidKeyException {\n"
                                                + "        cipher.init(Cipher.ENCRYPT_MODE, key, random);\n"
                                                + "    }\n"
                                                + "\n"
                                                + "    public void setup(String transform) throws NoSuchPaddingException, NoSuchAlgorithmException {\n"
                                                + "        Cipher cipher = Cipher.getInstance(transform);\n"
                                                + "    }\n"
                                                + "}\n"),
                                0xce82aa68,
                                "test/pkg/CipherTest1.class:"
                                        + "H4sIAAAAAAAAAI1S2U7bQBQ9Q7xAaghlSxegDaE0EIrV9qEPVJUq1FYRSyuC"
                                        + "eHfskTM0GVv2GJHP6gsgHvoB/aiqd2JUkihVseUzvnfuOXeZ+fX75ieAd3hZ"
                                        + "hI1HNh4XMYEnGp5qWLaxYmOVwXovpFAfGAq1zVMGYy8KOEPpQEh+lHVbPDnx"
                                        + "Wh3ymIqn6jVDvXZw5p17F66f9GIVuXsibvNkt+90U+5niVA9d5/3drVc8dOF"
                                        + "z2MlIpnaeHar8obhy71VRlxN/cOPPRlE3X4GM+UqixkWc0W348nQbapEyJD2"
                                        + "bTy3UaE6mlGW+Pyz0K3M5tlOdEM7muRgElMM82NKcrCGqoZ1By+wwbCgO3Dj"
                                        + "76E7oEKad8m/ts64rxgqw5U35LnXEQH19HcmDNWhlEdRM/Pb37wgoOoHojaG"
                                        + "lfKwj50wIrPdHQg09FkylGuNf5zH27E7/5vxg5CrhkyVJ30a3+a4SY89TlRg"
                                        + "0e3TjwGmh0xYJGuFVkaruXUF9oN+KAeh1Xc6hA6m6ZLq0CoK9AJ23dq+ROEu"
                                        + "WN9iYIZIJcI8Q4m+WTy8pa6Tz6B1sm5sv7qEMcqdo9rmh7hzZOfcNbJ0Wqt+"
                                        + "DfNwlLkEE+U+0yJ7AYu0u0TWBMp/AOFtLn55AwAA"))
                .run()
                .expect(expected);
    }

    public void testGetArity() {
        assertEquals(2, SecureRandomGeneratorDetector.getDescArity("(ILjava/security/Key;)V"));
        assertEquals(0, SecureRandomGeneratorDetector.getDescArity("()V"));
        assertEquals(1, SecureRandomGeneratorDetector.getDescArity("(I)V"));
        assertEquals(
                3,
                SecureRandomGeneratorDetector.getDescArity(
                        "(Ljava/lang/String;Ljava/lang/String;I)V"));
        assertEquals(0, SecureRandomGeneratorDetector.getDescArity("()Lfoo/bar/Baz;"));
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mPrngCalls =
            compiled(
                    "bin/classes",
                    java(
                            ""
                                    + "package test.pkg;\n"
                                    + "\n"
                                    + "import java.security.KeyPairGenerator;\n"
                                    + "import java.security.NoSuchAlgorithmException;\n"
                                    + "import java.security.NoSuchProviderException;\n"
                                    + "import java.security.SecureRandom;\n"
                                    + "\n"
                                    + "import javax.crypto.KeyAgreement;\n"
                                    + "import javax.crypto.KeyGenerator;\n"
                                    + "\n"
                                    + "public class PrngCalls {\n"
                                    + "    public void testKeyGenerator() throws NoSuchAlgorithmException, NoSuchProviderException {\n"
                                    + "        KeyGenerator generator = KeyGenerator.getInstance(\"AES\", \"BC\");\n"
                                    + "        generator.init(128);\n"
                                    + "\n"
                                    + "        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(\"RSA\");\n"
                                    + "        keyGen.initialize(512);\n"
                                    + "\n"
                                    + "        KeyAgreement agreement = KeyAgreement.getInstance(\"DH\", \"BC\");\n"
                                    + "        agreement.generateSecret();\n"
                                    + "\n"
                                    + "        SecureRandom random = new SecureRandom();\n"
                                    + "        byte bytes[] = new byte[20];\n"
                                    + "        random.nextBytes(bytes);\n"
                                    + "    }\n"
                                    + "}\n"),
                    0x1326b88d,
                    ""
                            + "test/pkg/PrngCalls.class:"
                            + "H4sIAAAAAAAAAJ1SXU8TQRQ9s7TdZVkKXUEpX4oCtuVj/UDBgmKLqMSKhBqM"
                            + "4Wm7TJbFdpdMp4QaH/xLvhSixh/gjzLeKQkfEV7ch3Nnzt4759y58/vP918A"
                            + "5vHURC+GDAwbGDExipsKbhkYM3EbdxSMG5gwMYm7CjI6siYsDCnI6ZjSMc2Q"
                            + "WArCQD5j6MhktxhiK9EOZ+gpBSFfb9QqXLx3K1VieiWvyze8+YqHXLgyEgzm"
                            + "6qHH92UQhXUdMzpmiSpHDeHxl4EqSW6I0F9xq9X67J574FpIwSadwmqZQSuu"
                            + "6HAs3MN9Cw/wkPjNckHHnKIeWXisKO3Fax3zilmw8AR5hkF1kFPnXkMEsumU"
                            + "1YJvuuFOVLOwiCUGW/l09j/5zqk6mW+XVd3Qd95V9rgnGSYvnrQelRvebqHq"
                            + "R7TdrZ12xjBxWeKGiA6CHS7O5aVV3qHjiea+jJyLN9Xlc7kW1qUbenQvhUzp"
                            + "zE9ZiiD0F/9lsqUrD1ykOampUcisqamNXvRIuRtuIM4ZmLtE8kTg6ipSMZVK"
                            + "4FaDz/ySBgu+4LzGQ/n/LZ0eQWJJ/0SZ01QFbzeX3S4ydIb8UBabNFeGeGa7"
                            + "mN3CGHro6atPA1PPivAa7UYoMorx3BHYN1ow9BEm2mQXYT+lau3ULxQTFJ/b"
                            + "mt1xjFhpKoWvLcTtxDH0t9MpDS0Ydqf6Z67PtND14Qesj0fozsd6+34a+Xg6"
                            + "lo63kDxTycEk7IaBZNtePxkbJq1x+pvFdXrlN7CAASwjTTvlSiNmgKrT7UYG"
                            + "/wIFnqm92QMAAA==");
}
