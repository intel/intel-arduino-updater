/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

package com.intel.galileo.flash.tool;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalileoVersion implements Comparable<GalileoVersion> {

    static Charset utf8 = Charset.forName("UTF-8");
    static Integer zero = 0;
    static Integer[] empty = { };
    static String quarkCodeline = "!Quark";

    private final String codeline;
    private final List<Integer> release;
    private final Integer build;

    public GalileoVersion(String codeline, Integer[] release, Integer build) {
        this.codeline = new String(codeline.getBytes(utf8));
        this.release = Arrays.asList(release);
        this.build = build;
    }

    private String toStringAux(Boolean useCanonical) {
        StringBuilder s = new StringBuilder();

        Boolean codelined = (this.codeline.length() > 0);
        Boolean identified = (this.release.size() > 0);
        Boolean serialized = !this.build.equals(zero);

        if (!useCanonical && this.codeline.equals(quarkCodeline)) {
            codelined = false;
            serialized = !identified;
        }

        if (codelined) {
            s.append(this.codeline);

            if (identified) {
                s.append('-');
            }
        }

        Boolean point = false;
        for (Integer n : this.release) {
            if (point) s.append('.');
            s.append(n.toString());
            point = true;
        }

        if (serialized) {
            if (identified || codelined) {
                s.append('+');
            }

            s.append(this.build.toString());
        }

        return s.toString();
    }

    public String toPresentationString() {
        return this.toStringAux(false);
    }

    public String toCanonicalString() {
        return this.toStringAux(true);
    }

    static public boolean isReleasedQuarkBuild(String version) {
        Boolean matched = version.matches("^0[Xx][0-9A-Fa-f]+$");

        if (matched) {
            int n = Integer.parseInt(version.substring(2), 16);
            if (n < 0x80000) {
                matched = false;
            }
        }

        return matched;
    }

    static public boolean isUnreleasedQuarkBuild(String version) {
        Boolean matched = version.matches("^[1-9][0-9]*$");

        if (!matched) {
            matched = version.matches("^0[Xx][0-9A-Fa-f]+$");

            if (matched) {
                int n = Integer.parseInt(version.substring(2), 16);
                if (n >= 0x80000) {
                    matched = false;
                }
            }
        }

        return matched;
    }

    static public boolean isCanonicalFormat(String version) {
        Pattern p;
        Matcher m;
        Boolean hasReleaseArray;
        Boolean hasBuildNumber;

        if (version == null || version.length() < 1 || version.length() > 63) {
            return false;
        }

        p = Pattern.compile("(!?[A-Z][A-Za-z0-9_]*)([-+]?)");
        m = p.matcher(version);
        if (!m.lookingAt()) {
            return false;
        }

        version = version.substring(m.end());
        String conjunct = m.group(1);
        hasReleaseArray = conjunct.equals("-");
        hasBuildNumber = conjunct.equals("+");

        if (hasReleaseArray) {
            p = Pattern.compile("((0|[1-9][0-9]*)(\\.(0|[1-9][0-9]*)))(\\+?)");
            m = p.matcher(version);
            if (!m.lookingAt()) {
                return false;
            }

            version = version.substring(m.end());
            conjunct = m.group(4);
            hasBuildNumber = conjunct.equals("+");
        }

        return !hasBuildNumber || version.matches("0|[1-9][0-9]*");
    }

    static public String parseCodeline(String version) {
        int i = version.indexOf("-");
        if (i < 0) {
            i = version.indexOf("+");
            if (i < 0) {
                return "";
            }
        }

        return version.substring(0, i);
    }

    static public Integer parseBuildNumber(String version) {
        int i = version.lastIndexOf("+");
        if (i < 0) {
            return zero;
        }

        try {
            String s = version.substring(i + 1);
            return Integer.parseInt(s, 10);
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(version);
        }
    }

    static public Integer[] parseReleaseArray(String version) {
        int i = version.indexOf("-");
        i = (i >= 0) ? i + 1 : 0;

        int j = version.lastIndexOf("+");
        j = (j >= 0) ? j : version.length();

        List<Integer> v = new ArrayList<Integer>();
        if (i < j) {
            String code = version.substring(i, j);
            for (String part : code.split("\\.")) {
                Integer n;

                try {
                    n = Integer.parseInt(part, 10);
                }
                catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("part '" + part + "' not valid.");
                }

                v.add(n);
            }
        }

        return v.toArray(empty);
    }

    static public GalileoVersion ofTargetString(String version) {
        if (GalileoVersion.isUnreleasedQuarkBuild(version)) {
            String codeline = quarkCodeline;
            Integer build;
            Integer[] release = empty;

            if (version.startsWith("0x") || version.startsWith("0X")) {
                build = Integer.parseInt(version.substring(2), 16);
            }
            else {
                build = Integer.parseInt(version, 10);
            }

            return new GalileoVersion(codeline, release, build);
        }
        else if (GalileoVersion.isReleasedQuarkBuild(version)) {
            String codeline = quarkCodeline;

            int n = Integer.parseInt(version.substring(2), 16);

            Integer build = n & 0xff;
            n >>= 8;

            List<Integer> v = new ArrayList<Integer>();
            for (int i = 0; i < 3; ++i) {
                v.add(0, n & 0xff);
                n >>= 8;
            }

            Integer[] release = v.toArray(empty);

            return new GalileoVersion(codeline, release, build);
        }

        return ofString(version);
    }

    static public GalileoVersion ofString(String version) {
        if (!GalileoVersion.isCanonicalFormat(version)) {
            throw new IllegalArgumentException(version);
        }

        String codeline = GalileoVersion.parseCodeline(version);
        Integer[] release = GalileoVersion.parseReleaseArray(version);
        Integer build = GalileoVersion.parseBuildNumber(version);

        return new GalileoVersion(codeline, release, build);
    }

    @Override
    public int compareTo(GalileoVersion that) {
        Iterator<Integer> thisRelease = this.release.listIterator();
        Iterator<Integer> thatRelease = that.release.listIterator();

        if (this.codeline.equals(quarkCodeline) && that.codeline.equals(quarkCodeline)) {
            boolean thisHasNext = thisRelease.hasNext();
            boolean thatHasNext = thatRelease.hasNext();

            if (thisHasNext != thatHasNext)
                throw new IllegalArgumentException();
        }

        while (thisRelease.hasNext() && thatRelease.hasNext()) {
            Integer thisPart = thisRelease.next();
            Integer thatPart = thatRelease.next();
            int d = thisPart.compareTo(thatPart);
            if (d != 0) {
                return d;
            }
        }

        if (thisRelease.hasNext()) {
            return -1;
        }

        if (thatRelease.hasNext()) {
            return 1;
        }

        int d = this.codeline.compareTo(that.codeline);
        if (d != 0) {
            return d;
        }

        return this.build.compareTo(that.build);
    }
};

//--- End of file
