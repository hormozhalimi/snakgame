package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.MotionEvent;

import java.util.ArrayList;

class Snake  implements Drawable, Collidable  {

    // The location in the grid of all the segments
    private ArrayList<Point> segmentLocations;

    // How big is each segment of the snake?
    private int mSegmentSize ;

    // How big is the entire grid
    private Point mMoveRange;

    // Where is the centre of the screen
    // horizontally in pixels?
    private int halfWayPoint;

    // For tracking movement Heading
    private enum Heading {
        UP, RIGHT, DOWN, LEFT
    }

    // Start by heading to the right
    private Heading heading = Heading.RIGHT;

    // A bitmap for each direction the head can face
    private Bitmap mBitmapHeadRight;
    private Bitmap mBitmapHeadLeft;
    private Bitmap mBitmapHeadUp;
    private Bitmap mBitmapHeadDown;

    // A bitmap for the body
    private Bitmap mBitmapBody;


    Snake(Context context, Point mr, int ss) {
        super();

        // Initialize our ArrayList
        segmentLocations = new ArrayList<>();

        // Initialize the segment size and movement
        // range from the passed in parameters
        mSegmentSize = ss;

        mMoveRange = mr;


        // Create and scale the bitmaps
        mBitmapHeadRight = BitmapFactory.decodeResource(context.getResources(), R.drawable.head);

// Create 3 more versions of the head for different headings
        mBitmapHeadLeft = BitmapFactory.decodeResource(context.getResources(), R.drawable.head);

        mBitmapHeadUp = BitmapFactory.decodeResource(context.getResources(), R.drawable.head);

        mBitmapHeadDown = BitmapFactory .decodeResource(context.getResources(), R.drawable.head);

// Modify the bitmaps to face the snake head
// in the correct direction
        mBitmapHeadRight = Bitmap.createScaledBitmap(mBitmapHeadRight, 150, 150, false);

// A matrix for scaling
        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1);

        mBitmapHeadLeft = Bitmap.createBitmap(mBitmapHeadRight, 0, 0, 150, 150, matrix, true);

// A matrix for rotating
        matrix.preRotate(-90);
        mBitmapHeadUp = Bitmap.createBitmap(mBitmapHeadRight, 0, 0, 150, 150, matrix, true);

// Matrix operations are cumulative
// so rotate by 180 to face down
        matrix.preRotate(180);
        mBitmapHeadDown = Bitmap.createBitmap(mBitmapHeadRight, 0, 0, 150, 150, matrix, true);

// Create and scale the body
        mBitmapBody = BitmapFactory.decodeResource(context.getResources(), R.drawable.body);

        mBitmapBody = Bitmap.createScaledBitmap(mBitmapBody, 120, 120, false);

        // The halfway point across the screen in pixels
        // Used to detect which side of screen was pressed
        halfWayPoint = mr.x * ss / 2;
    }

    // Get the snake ready for a new game
    void reset(int w, int h) {

        // Reset the heading
        heading = Heading.RIGHT;

        // Delete the old contents of the ArrayList
        segmentLocations.clear();

        // Start with a single snake segment
        segmentLocations.add(new Point(w / 2, h / 2));
    }




    boolean detectDeath() {
        // Has the snake died?
        boolean dead = false;

        // Hit any of the screen edges
        if (segmentLocations.get(0).x < 0 || segmentLocations.get(0).x >= mMoveRange.x ||
                segmentLocations.get(0).y < 0 || segmentLocations.get(0).y >= mMoveRange.y) {
            dead = true;
        }

        // Eaten itself?
        for (int i = segmentLocations.size() - 1; i > 0; i--) {
            // Have any of the sections collided with the head
            if (segmentLocations.get(0).x == segmentLocations.get(i).x &&
                    segmentLocations.get(0).y == segmentLocations.get(i).y) {
                dead = true;
            }
        }

        return dead;
    }


    public boolean checkCollision(Point l) {
        // Calculate the range for collision detection
        int halfAppleSize = 40; // Half of the apple size
        int halfHeadSize = 40; // Half of the snake's head size

        // Calculate the center points of the snake's head and the apple
        int headCenterX = segmentLocations.get(0).x * mSegmentSize + halfHeadSize;
        int headCenterY = segmentLocations.get(0).y * mSegmentSize + halfHeadSize;
        int appleCenterX = l.x * mSegmentSize + halfAppleSize;
        int appleCenterY = l.y * mSegmentSize + halfAppleSize;

        // Calculate the distance between the centers of the snake's head and the apple
        int distanceX = Math.abs(headCenterX - appleCenterX);
        int distanceY = Math.abs(headCenterY - appleCenterY);

        // Check if the distance between the centers is within the collision range
        if (distanceX < halfAppleSize + halfHeadSize && distanceY < halfAppleSize + halfHeadSize) {
            // Add a new Point to the list
            // located off-screen.
            // This is OK because on the next call to
            // move it will take the position of
            // the segment in front of it
            segmentLocations.add(new Point(-10, -10));
            return true;
        }
        return false;
    }



    public void move() {
        // Move the body
        for (int i = segmentLocations.size() - 1; i > 0; i--) {
            // Move each segment to the position of the segment in front of it
            segmentLocations.get(i).x = segmentLocations.get(i - 1).x;
            segmentLocations.get(i).y = segmentLocations.get(i - 1).y;
        }

        // Move the head in the appropriate heading
        Point p = new Point(segmentLocations.get(0).x, segmentLocations.get(0).y);
        switch (heading) {
            case UP:
                p.y--;
                break;
            case RIGHT:
                p.x++;
                break;
            case DOWN:
                p.y++;
                break;
            case LEFT:
                p.x--;
                break;
        }
        segmentLocations.set(0, p);
    }


    public void draw(Canvas canvas, Paint paint) {
        if (!segmentLocations.isEmpty()) {
            // Draw the snake body first, starting from the end of the list
            for (int i = segmentLocations.size() - 1; i > 0; i--) {
                int x = segmentLocations.get(i).x * mSegmentSize;
                int y = segmentLocations.get(i).y * mSegmentSize;
                canvas.drawBitmap(mBitmapBody, x, y, paint);
            }

            // Draw the head on top
            switch (heading) {
                case RIGHT:
                    canvas.drawBitmap(mBitmapHeadRight,
                            segmentLocations.get(0).x * mSegmentSize,
                            segmentLocations.get(0).y * mSegmentSize, paint);
                    break;
                case LEFT:
                    canvas.drawBitmap(mBitmapHeadLeft,
                            segmentLocations.get(0).x * mSegmentSize,
                            segmentLocations.get(0).y * mSegmentSize, paint);
                    break;
                case UP:
                    canvas.drawBitmap(mBitmapHeadUp,
                            segmentLocations.get(0).x * mSegmentSize,
                            segmentLocations.get(0).y * mSegmentSize, paint);
                    break;
                case DOWN:
                    canvas.drawBitmap(mBitmapHeadDown,
                            segmentLocations.get(0).x * mSegmentSize,
                            segmentLocations.get(0).y * mSegmentSize, paint);
                    break;
            }
        }
    }





    // Handle changing direction
    void switchHeading(MotionEvent motionEvent) {

        // Is the tap on the right hand side?
        if (motionEvent.getX() >= halfWayPoint) {
            switch (heading) {
                // Rotate right
                case UP:
                    heading = Heading.RIGHT;
                    break;
                case RIGHT:
                    heading = Heading.DOWN;
                    break;
                case DOWN:
                    heading = Heading.LEFT;
                    break;
                case LEFT:
                    heading = Heading.UP;
                    break;

            }
        } else {
            // Rotate left
            switch (heading) {
                case UP:
                    heading = Heading.LEFT;
                    break;
                case LEFT:
                    heading = Heading.DOWN;
                    break;
                case DOWN:
                    heading = Heading.RIGHT;
                    break;
                case RIGHT:
                    heading = Heading.UP;
                    break;
            }
        }
    }


}
