package com.example.easyqc3;

import org.opencv.core.Point;

public class ArucoMarker {
    private int id;
    private Point[] corners;

    public ArucoMarker(int id, Point[] corners) {
        this.id = id;
        this.corners = corners;
    }

    public int getId() {
        return id;
    }

    public Point[] getCorners() {
        return corners;
    }
}