package com.example.myapplication;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;

class SnakeGame extends SurfaceView implements Runnable {

    // Objects for the game loop/thread
    private Thread mThread = null;
    // Control pausing between updates
    private long mNextFrameTime;
    // Is the game currently playing and or paused?
    private volatile boolean mPlaying = false;
    private volatile boolean mPaused = true;
    private boolean pausedByButton = false;
    // for playing sound effects
    private SoundPool mSP;
    private int mEat_ID = -1;
    private int mCrashID = -1;

    // The size in segments of the playable area
    private final int NUM_BLOCKS_WIDE = 40;
    private int mNumBlocksHigh;

    // How many points does the player have
    private int mScore;

    // Objects for drawing
    private Canvas mCanvas;
    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;

    // A snake ssss
    private Snake mSnake;
    // And an apple
    private Apple mApple;
    private Bitmap mBitmapPause;
    private Bitmap mBackgroundBitmap;
    private boolean mGameStarted = false;


    // This is the constructor method that gets called
    // from SnakeActivity

    public SnakeGame(Context context, Point size) {
        super(context);
        int blocksize = 50;
        // Work out how many pixels each block is
        int blockSize = size.x / NUM_BLOCKS_WIDE;
        // How many blocks of the same size will fit into the height
        mNumBlocksHigh = size.y / blockSize;

        // Initialize the SoundPool
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            mSP = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            mSP = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }
        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Prepare the sounds in memory
            descriptor = assetManager.openFd("get_apple.ogg");
            mEat_ID = mSP.load(descriptor, 0);

            descriptor = assetManager.openFd("snake_death.ogg");
            mCrashID = mSP.load(descriptor, 0);

        } catch (IOException e) {
            // Error
        }

        // Initialize the drawing objects
        mSurfaceHolder = getHolder();
        mPaint = new Paint();

        // Call the constructors of our two game objects
        mApple = new Apple(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize);
        mSnake = new Snake(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize);

        mBitmapPause = BitmapFactory.decodeResource(getResources(), R.drawable.pause_icon);
        mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.background_image);

    }


    // Called to start a new game



    // Handles the game loop
    @Override
    public void run() {
        while (mPlaying) {
            if (!mPaused) {
                // Update 10 times a second
                if (updateRequired()) {
                    update();
                }
            }

            draw();
        }
    }


    // Check to see if it is time for an update
    public boolean updateRequired() {

        // Run at 10 frames per second
        final long TARGET_FPS = 10;
        // There are 1000 milliseconds in a second
        final long MILLIS_PER_SECOND = 1000;

        // Are we due to update the frame
        if (mNextFrameTime <= System.currentTimeMillis()) {
            // Tenth of a second has passed

            // Setup when the next update will be triggered
            mNextFrameTime = System.currentTimeMillis()
                    + MILLIS_PER_SECOND / TARGET_FPS;

            // Return true so that the update and draw
            // methods are executed
            return true;
        }

        return false;
    }


    // Update all the game objects
    public void update() {

        // Move the snake
        mSnake.move();

        // Did the head of the snake eat the apple?
        if (mSnake.checkCollision(mApple.getLocation())) {
            // This reminds me of Edge of Tomorrow.
            // One day the apple will be ready!
            mApple.spawn();

            // Add to  mScore
            mScore = mScore + 1;

            // Play a sound
            mSP.play(mEat_ID, 1, 1, 0, 0, 1);
        }

        // Did the snake die?
        if (mSnake.detectDeath()) {
            // Pause the game ready to start again
            mSP.play(mCrashID, 1, 1, 0, 0, 1);

            mPaused = true;
        }

    }


    // Called to start a new game
    public void newGame() {
        // reset the snake
        mSnake.reset(NUM_BLOCKS_WIDE, mNumBlocksHigh);

        // Get the apple ready for dinner
        mApple.spawn();

        // Reset the mScore
        mScore = 0;

        // Setup mNextFrameTime so an update can triggered
        mNextFrameTime = System.currentTimeMillis();

        // Set the game started flag to true
        mGameStarted = true;
    }

    // Do all the drawing
    public void draw() {
        if (mSurfaceHolder.getSurface().isValid()) {
            mCanvas = mSurfaceHolder.lockCanvas();

            // Clear the canvas
            mCanvas.drawColor(Color.BLACK);

            // Calculate the scale factors for stretching the background image
            float scaleX = (float) mCanvas.getWidth() / mBackgroundBitmap.getWidth();
            float scaleY = (float) mCanvas.getHeight() / mBackgroundBitmap.getHeight();

            // Create a Matrix for scaling
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.setScale(scaleX, scaleY);

            // Apply the scale to the background bitmap
            Bitmap scaledBackgroundBitmap = Bitmap.createBitmap(mBackgroundBitmap, 0, 0, mBackgroundBitmap.getWidth(), mBackgroundBitmap.getHeight(), matrix, true);

            // Draw the scaled background bitmap
            mCanvas.drawBitmap(scaledBackgroundBitmap, 0, 0, null);

            // Determine the text size of the score
            float scoreTextSize = 120;

            // Draw the score
            mPaint.setColor(Color.BLACK);
            mPaint.setTextSize(scoreTextSize);
            mCanvas.drawText("" + mScore, 50, scoreTextSize, mPaint);

            // Draw the apple and the snake
            mApple.draw(mCanvas, mPaint);
            mSnake.draw(mCanvas, mPaint);

            // Draw names in the top right corner
            mPaint.setTextSize(40);
            mCanvas.drawText("Your Name", mCanvas.getWidth() - 300, 50, mPaint);
            mCanvas.drawText("Partner's Name", mCanvas.getWidth() - 300, 100, mPaint);

            // Draw the pause button
            if (!pausedByButton) {
                // Calculate the scale to fit the icon based on the score text size
                float iconScale = scoreTextSize / (mBitmapPause.getWidth() * 2 ); // Smaller scale factor

                // Scale the pause icon
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(mBitmapPause,
                        (int) (mBitmapPause.getWidth() * iconScale),
                        (int) (mBitmapPause.getHeight() * iconScale),
                        true);

                // Adjust the coordinates to position the pause button
                int margin = 100; // Margin from top-right corner
                int iconX = mCanvas.getWidth() - scaledBitmap.getWidth() - margin; // X-coordinate
                int iconY = margin; // Y-coordinate
                mCanvas.drawBitmap(scaledBitmap, iconX, iconY, mPaint);
            }

            // Check if the game is paused and draw the appropriate text
            if (!mGameStarted) {
                mPaint.setColor(Color.BLACK);
                mPaint.setTextSize(150); // Adjust the text size as needed
                mPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                mCanvas.drawText("Tap to play", mCanvas.getWidth() / 2 - 300, mCanvas.getHeight() / 2, mPaint);
            } else if (mSnake.detectDeath()) {
                mPaint.setColor(Color.BLACK);
                mPaint.setTextSize(150); // Adjust the text size as needed
                mPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                mCanvas.drawText("Tap to Restart", mCanvas.getWidth() / 2 - 450, mCanvas.getHeight() / 2, mPaint);
            } else if (mPaused) {
                mPaint.setColor(Color.BLACK);
                mPaint.setTextSize(200); // Adjust the text size as needed
                mPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                mCanvas.drawText("Paused", mCanvas.getWidth() / 2 - 400, mCanvas.getHeight() / 2, mPaint);

            }

            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
        }
    }










    // Modify onTouchEvent() method to handle pause button

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        // Calculate the scaled icon size and position based on the draw method logic
        float scoreTextSize = 120;
        float iconScale = scoreTextSize / (mBitmapPause.getWidth() * 2); // Assuming scoreTextSize is used for scaling
        int scaledIconWidth = (int) (mBitmapPause.getWidth() * iconScale);
        int scaledIconHeight = (int) (mBitmapPause.getHeight() * iconScale);
        int margin = 100; // Margin from top-right corner
        int iconX = mCanvas.getWidth() - scaledIconWidth - margin; // X-coordinate
        int iconY = margin; // Y-coordinate

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // Check if the touch started within the bounds of the scaled pause button icon
                if (motionEvent.getX() > iconX &&
                        motionEvent.getX() < iconX + scaledIconWidth &&
                        motionEvent.getY() > iconY &&  // Ensure touch is below the pause button
                        motionEvent.getY() < iconY + scaledIconHeight) {
                    // Set a flag to indicate touch started on the pause button
                    pausedByButton = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                // Check if the touch ended within the bounds of the scaled pause button icon and if it started on the pause button
                if (motionEvent.getX() > iconX &&
                        motionEvent.getX() < iconX + scaledIconWidth &&
                        motionEvent.getY() > iconY &&  // Ensure touch is below the pause button
                        motionEvent.getY() < iconY + scaledIconHeight &&
                        pausedByButton) {
                    // Toggle pause state only if the touch started and ended on the pause button
                    mPaused = !mPaused;
                } else if (mPaused && !pausedByButton) {
                    // Resume the game and start a new game if it was paused and not paused by button click
                    mPaused = false;
                    newGame();
                } else {
                    // Let the Snake class handle the input
                    mSnake.switchHeading(motionEvent);
                }
                // Reset the flag indicating touch by the button
                pausedByButton = false;
                break;
        }
        return true;
    }







    // Stop the thread
    public void pause() {
        mPlaying = false;
        if (!pausedByButton) {
            // Reset the game only if it's not paused by a button click
            newGame();
        }
        try {
            mThread.join();
        } catch (InterruptedException e) {
            // Error
        }
    }

    // resume() method
    public void resume() {
        mPlaying = true;
        mThread = new Thread(this);
        mThread.start();
    }
}