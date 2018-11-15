package com.example.rohitkumarbhamu.pikasso.view;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.UriMatcher;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class PikassoView extends View {

    public static final float TOUCH_TOLERANCE=10;
    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private Paint paintScreen;
    private Paint paintLine;
    private HashMap<Integer,Path>  pathMap;
    private  HashMap<Integer,Point> previousPointMap;
    public String filename;
    //Trying to save the state to store teh



    public PikassoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    void  init(){
        paintScreen=new Paint();
        paintLine= new Paint();
        paintLine.setAntiAlias(true);
        paintLine.setColor(Color.BLACK);
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(5);
        paintLine.setStrokeCap(Paint.Cap.ROUND);

        pathMap=new HashMap<>();
        previousPointMap=new HashMap<>();

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        bitmap=Bitmap.createBitmap(getWidth(),getHeight(), Bitmap.Config.ARGB_8888);
        bitmapCanvas=new Canvas(bitmap);
        bitmap.eraseColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(bitmap,0,0,paintScreen);

        for (Integer key:pathMap.keySet()){

            canvas.drawPath(pathMap.get(key),paintLine);
        }

    }




    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action=event.getActionMasked();
        int actionIndex=event.getActionIndex();

        if (action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN){
            touchStarted(event.getX(actionIndex),
                    event.getY(actionIndex),
                    event.getPointerId(actionIndex));

            Log.d("TEST:",String.valueOf(event.getPointerId(actionIndex)));

        }
        else if (action==MotionEvent.ACTION_UP|| action==MotionEvent.ACTION_POINTER_UP){
            touchEnded(event.getPointerId(actionIndex));

            Log.d("TEST:",String.valueOf(event.getPointerId(actionIndex)));

        }
        else {
            touchMoved(event);
        }
        invalidate();//redraw the screen

        return true;
    }


    private void touchMoved(MotionEvent event) {
        for (int i=0;i<event.getPointerCount();i++){
            int pointerId=event.getPointerId(i);
            int pointerIndex=event.findPointerIndex(pointerId);

            if (pathMap.containsKey(pointerId)){
                float newX=event.getX(pointerIndex);
                float newY=event.getY(pointerIndex);

                Path path=pathMap.get(pointerId);
                Point point=previousPointMap.get(pointerId);


                //Calculate far the user moved from last update

                float deltaX=Math.abs(newX-point.x);
                float deltaY=Math.abs(newY-point.y);

                //if he distance is significant enough to be considered
                if (deltaX>= TOUCH_TOLERANCE|| deltaY>= TOUCH_TOLERANCE){
                    //move the path to the new location
                    path.quadTo(point.x,point.y,(newX+point.x)/2,(newY+point.y)/2);

                    //save the new point
                    point.x=(int)newX;
                    point.y=(int)newY;



                }

            }
        }

    }






    public void setDrawingColor(int color){
        paintLine.setColor(color);

    }

    public int getDrawingColor()
    {
        return paintLine.getColor();

    }

    public void setLineWidth(int width){
        paintLine.setStrokeWidth(width);

    }
    public int getLineWidth(){
        return (int) paintLine.getStrokeWidth();
    }

     public void  clear(){
        pathMap.clear();//removes all the paths
         previousPointMap.clear();
         bitmap.eraseColor(Color.WHITE);
         invalidate();//refresh the screen

     }




    private  void touchEnded(int pointerId) {
        Path path =pathMap.get(pointerId);//get the corresponding path
        bitmapCanvas.drawPath(path,paintLine);//draw to bitmapCanvas
        path.reset();
    }

    private void touchStarted(float x, float y, int pointerId) {
        Path path;      //store the path for the touch
        Point point;     // store the last point on the path

        if (pathMap.containsKey(pointerId)){

            path=pathMap.get(pointerId);
            point=previousPointMap.get(pointerId);
        }
        else {
            path = new Path();
            pathMap.put(pointerId,path);
            point=new Point();
            previousPointMap.put(pointerId,point);

        }
        path.moveTo(x,y);
        point.x=(int)x;
        point.y=(int)y;


    }
   /*  public void saveImage(){
        String filename="Pikasso"+System.currentTimeMillis();

        ContentValues values= new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,filename);
        values.put(MediaStore.Images.Media.DATE_ADDED,System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpg");


        //get a URI for the location to save the file
        Uri uri =getContext().getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI,values);

        try {
            OutputStream outputStream=
                    getContext().getContentResolver().openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG,500,outputStream);//this is our Image

            outputStream.flush();
            outputStream.close();

            Toast message=  Toast.makeText(getContext(),"Image Saved",Toast.LENGTH_SHORT);
            message.setGravity(Gravity.CENTER,message.getXOffset()/2,message.getYOffset()/2);
            message.show();

        } catch (FileNotFoundException e) {

            Toast message=  Toast.makeText(getContext(),"Image Not Saved",Toast.LENGTH_SHORT);
            message.setGravity(Gravity.CENTER,message.getXOffset()/2,message.getYOffset()/2);
            message.show();
            e.printStackTrace();
        } catch (IOException e) {
            Toast message=  Toast.makeText(getContext(),"Image Not Saved",Toast.LENGTH_SHORT);
            message.setGravity(Gravity.CENTER,message.getXOffset()/2,message.getYOffset()/2);
            message.show();

            e.printStackTrace();
        }

    }

   public void saveImageToExternalStorage(){
        String filename= "Pikasso"+System.currentTimeMillis();

        ContentValues values= new ContentValues();


        values.put(MediaStore.Images.Media.TITLE,filename);
        values.put(MediaStore.Images.Media.DATE_ADDED,System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpg");

        //get a URI For the location to save the file

        Uri uri=getContext().getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI,values);

        try {
            getContext().getContentResolver().openOutputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
       // File file = new File(Environment.getExternalStorageDirectory());


    }*/

    public void saveToInternalStorage(){
        ContextWrapper cw = new ContextWrapper(getContext());
        filename="Pikasso"+System.currentTimeMillis();

        //path to /data/data/yourapp/app_data/imageDir
        File directory=cw.getDir("imageDir",Context.MODE_PRIVATE);




        //Create Image Dir
        File mypath=new File(directory,filename+".jpg");

        FileOutputStream fos=null;


        try {
            fos=new FileOutputStream(mypath);
            //use the compress method on the Bitmap object to write image to the output stram
            bitmap.compress(Bitmap.CompressFormat.PNG,100,fos);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            try {
                fos.flush();
                fos.close();
                Log.d("Image:",directory.getAbsolutePath());
                Toast message=Toast.makeText(getContext(),"Image Saved +" +directory.getAbsolutePath(),Toast.LENGTH_LONG);
                message.setGravity(Gravity.CENTER,message.getXOffset()/2,message.getYOffset()/2);
                message.show();
            } catch (IOException e) {

                Toast message=Toast.makeText(getContext(),"Image Not Saved ",Toast.LENGTH_SHORT);
                message.setGravity(Gravity.CENTER,message.getXOffset()/2,message.getYOffset()/2);
                message.show();
                e.printStackTrace();
            }
        }
    }


    /*
    public void loadImageFromStorage(String path){

        try {
            File f=new File(path,filename+".jpg");
            Bitmap b= BitmapFactory.decodeStream(new FileInputStream(f));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
*/

}
