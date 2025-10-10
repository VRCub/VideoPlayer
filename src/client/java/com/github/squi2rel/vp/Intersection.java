package com.github.squi2rel.vp;

import com.github.squi2rel.vp.video.ClientVideoScreen;
import org.joml.Vector3f;

public class Intersection {
    public static Result intersect(Vector3f lineStart, Vector3f lineEnd, ClientVideoScreen player) {
        Vector3f p1 = player.p1, p2 = player.p2, p3 = player.p3, p4 = player.p4;

        Result result = new Result();
        Vector3f edge1 = new Vector3f();
        Vector3f edge2 = new Vector3f();
        Vector3f normal = new Vector3f();
        p2.sub(p1, edge1);
        p3.sub(p1, edge2);
        edge1.cross(edge2, normal);
        normal.normalize();
        float d = normal.dot(p1);
        Vector3f lineDir = new Vector3f();
        lineEnd.sub(lineStart, lineDir);
        float length = lineDir.length();
        lineDir.normalize();

        float denom = normal.dot(lineDir);
        if (denom >= -0.000001f) return result;
        float distance = (d - normal.dot(lineStart)) / denom;
        if (distance < 0 || distance > length) return result;

        result.point.set(lineDir).mul(distance).add(lineStart);
        if (inTri(result.point, p1, p2, p3) || inTri(result.point, p1, p3, p4)) {
            result.intersects = true;
            result.distance = distance;
            result.screen = player;
        }

        return result;
    }

    private static boolean inTri(Vector3f p, Vector3f a, Vector3f b, Vector3f c) {
        // Barycentric Technique
        Vector3f v0 = new Vector3f(c).sub(a);
        Vector3f v1 = new Vector3f(b).sub(a);
        Vector3f v2 = new Vector3f(p).sub(a);

        float dot00 = v0.dot(v0);
        float dot01 = v0.dot(v1);
        float dot02 = v0.dot(v2);
        float dot11 = v1.dot(v1);
        float dot12 = v1.dot(v2);

        float denom = (dot00 * dot11 - dot01 * dot01);
        if (denom == 0.0f) return false; // Degenerate triangle

        float invDenom = 1.0f / denom;
        float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        return (u >= 0) && (v >= 0) && (u + v <= 1);
    }

    public static class Result {
        public boolean intersects;
        public Vector3f point;
        public float distance;
        public ClientVideoScreen screen;

        public Result() {
            this.intersects = false;
            this.point = new Vector3f();
            this.distance = 0f;
        }
    }
}