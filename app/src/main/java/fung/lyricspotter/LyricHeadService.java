package fung.lyricspotter;

import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class LyricHeadService extends Service {

    private WindowManager mWindowManager;
    private View mChatHeadView;
    private View mCloseHeadView;
    private boolean deleteView;
    private SharedPreferences mSharedPreferences;

    public LyricHeadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Inflate the chat head layout we created
        mChatHeadView = LayoutInflater.from(this).inflate(R.layout.layout_lyric_head, null);

        mSharedPreferences = this.getSharedPreferences("LYRICS", 0);
        SharedPreferences.Editor e = mSharedPreferences.edit();
        e.putBoolean("delete", true);
        e.commit();

        // Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        // Specify the chat head position
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        // Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mChatHeadView, params);

        // Try to add a close button?
        mCloseHeadView = LayoutInflater.from(this).inflate(R.layout.close_lyric_head, null);

        // Add the view to the window.
        final WindowManager.LayoutParams temp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        // Specify the close position
        temp.gravity = Gravity.BOTTOM;
        temp.x = 0;
        temp.y = 200;

        // Drag and move chat head using user's touch action.
        final ImageView chatHeadImage = mChatHeadView.findViewById(R.id.chat_head_profile_iv);
        chatHeadImage.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

                        // get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        Log.e("Delete View Down", "False");
                        deleteView = false;

                        return true;
                    case MotionEvent.ACTION_UP:
                        // Calculate the position of the view
                        int newX = initialX + (int) (event.getRawX() - initialTouchX);
                        int newY = initialY + (int) (event.getRawY() - initialTouchY);

                        // Originally used ACTiON_DOWN, but soon realized it's
                        // almost impossible for user to go up and down right
                        // after each other using their finger.
                        if (Math.abs(newX - initialX) < 25 && Math.abs(newY - initialY) < 25) {
                            // Open the chat conversation click.
                            Intent intent = new Intent(LyricHeadService.this, LyricActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);

                            if (deleteView) {
                                mWindowManager.removeViewImmediate(mCloseHeadView);
                                deleteView = false;
                            }
                            // close the service and remove the chat heads
                            stopSelf();
                            return true;
                        }

                        // Check if the user released the chat head
                        // on top of the (X) image
                        int[] firstPosition = new int[2];
                        int[] secondPosition = new int[2];

                        mChatHeadView.getLocationOnScreen(firstPosition);
                        mCloseHeadView.getLocationOnScreen(secondPosition);

                        // Rect constructor parameters: left, top, right, bottom
                        Rect rectFirstView = new Rect(firstPosition[0], firstPosition[1],
                                firstPosition[0] + mChatHeadView.getMeasuredWidth(), firstPosition[1] + mChatHeadView.getMeasuredHeight());
                        Rect rectSecondView = new Rect(secondPosition[0], secondPosition[1],
                                secondPosition[0] + mCloseHeadView.getMeasuredWidth(), secondPosition[1] + mCloseHeadView.getMeasuredHeight());

                        Log.e("deleteView why true:", "" + deleteView);
                        if (deleteView) {
                            Log.e("Delete View: Up", "True");
                            mWindowManager.removeViewImmediate(mCloseHeadView);
                            Log.e("Past", "deleted");
                            deleteView = false;
                            if (rectFirstView.intersect(rectSecondView)) {
                                stopSelf();
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        // Add the view to the window
                        if (!deleteView) {
                            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                            mWindowManager.addView(mCloseHeadView, temp);
                            Log.e("Delete View: Move ", "True");

                            deleteView = true;
                        }
                        // Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mChatHeadView, params);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatHeadView != null) mWindowManager.removeView(mChatHeadView);
    }
}

