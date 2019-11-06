package com.example.tp1note;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;


public class TouchExample extends View {
    private static final String TAG = "TouchExample";
    private float mScale = 1f;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;;

    //Valeurs de référence : on applique pas les modifs directement sur les images,
    //on change dabord les valeurs de référence puis on applique les changements de ces valeurs
    //sur les images à l'écran (toutes les images n'ont pas la meme taille en fonction de si
    // on les voit à l'écran ou pas).
    int refWidth; //largeur de référence
    int refHeight; //hauteur de référence

    int minLine;//Premiere ligne de BM aue l'on voit a l'écran
    int maxLine;//Derniere ligne de BM aue l'on voit a l'écran
    int min;//Indice min de l'image à afficher, prend en compte le scroll et le dépassement
    int max;//Indice max de l'image à afficher, prend en compte le scroll et le dépassement

    int maxHeight; //Hauteur maximale d'une image a l'écran
    int displayWidth; //Largeur de l'ecran
    float aspectRatio =(float) 9/16;
    int displayHeight = 1700; //Hauteur de l'écran

    int maxIm = 7; //Nombre max d'images par ligne

    ArrayList<BitmapDrawable> bmDrawList; //Contient tous les BMD que l'on veut afficher
    ArrayList<Bitmap> bmList = new ArrayList<>(); //Contient tous les BM que l'on veut afficher

    int scrollOffset = 0; //Coefficient de décalage du au scroll

    private Paint mPaint;

    public TouchExample(Context context) {
        super(context);

        mPaint = new Paint();

        mGestureDetector = new GestureDetector(context, new ZoomGesture());
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGesture());

        //Obtention des images depuis la mémoire
        bmDrawList = imagesGetter.getBitmaps("/storage/emulated/0/DCIM/Camera");
        for(int i = 0; i < bmDrawList.size(); i++) bmList.add(bmDrawList.get(i).getBitmap());

        //Obtention dynamique de la largeur de l'écran (pour supporter l'inclinaison de l'écran)
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager()
                .getDefaultDisplay()
                .getMetrics(displayMetrics);
        displayWidth = displayMetrics.widthPixels;
        maxHeight =(int) (displayWidth * aspectRatio);

        refWidth = displayWidth;
        refHeight = maxHeight;

        init();
    }

    //Affiche dynamiquement les images en quadrillage
    @Override
    public void onDraw(Canvas canvas) {
        //Offset des images
        int top;
        int left;

        int nImageLine = displayWidth/refWidth; //Actuel nombre d'images par ligne
        int tmpTop = refHeight; //Hauteur du BM de reference
        int tmpLeft = refWidth; //Largeur du BM de reference

        //Determine la limite du scroll
        if (tmpTop * (bmList.size()/nImageLine) > displayHeight) //Est-ce-que images ne rentrent pas toutes a l'écran ?
        {
            int lastLine = -displayHeight + tmpTop * (bmList.size()/nImageLine + 1) - (displayHeight/tmpTop); //Position en Y de la derniere ligne d'iamge de la gallery
            if(scrollOffset > 0) //Empeche de scroll trop haut
                scrollOffset = 0;
            if(scrollOffset < -lastLine) //Empeche de scroll trop bas
                scrollOffset = -lastLine;
            minLine = -scrollOffset/tmpTop - 1;
            maxLine = minLine + displayHeight/tmpTop + 4;
        }
        else //Désactivation du scroll
        {
            minLine = 0;
            maxLine = bmList.size()/nImageLine + bmList.size()%nImageLine;
            scrollOffset = 0;
        }

        max = (maxLine * nImageLine) + (bmList.size() % nImageLine);
        min = (minLine-1) * nImageLine;
        if (max > bmList.size()) max = bmList.size();
        if(min < 0) min = 0;

        //Affichage des images que l'on peut voir a l'écran(plus une ligne en bas et en haut)
        for (int i = min; i < max; i++)
        {
            top = tmpTop * (i/nImageLine);
            left = tmpLeft * (i%nImageLine);
            bmList.set(i, bmDrawList.get(i).getBitmap());
            bmList.set(i,Bitmap.createScaledBitmap(bmList.get(i),refWidth, refHeight, false));
            canvas.drawBitmap(bmList.get(i), left, top + scrollOffset, mPaint);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);
        return true;
    }


    public class ZoomGesture extends GestureDetector.SimpleOnGestureListener {
        //Prise en compte du scroll
        @Override
        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,float distanceY) {
            scrollOffset -= distanceY;
            invalidate();
            return true;
        }
    }

    public class ScaleGesture extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            //La scale ne doit pas etre trop grande ou trop petite, pour éviter des mouvements inutiles
            mScale *= detector.getScaleFactor();
            mScale = (mScale > 1) ? 1 : mScale;
            mScale = (mScale < 1./9) ? (float) 1/9 : mScale;

            setRef(mScale); //On applique les transfos sur les valeurs de reference

            //On applique les changements des valeurs de reference sur toutes les images à afficher
            for (int i = min; i < max; i++)
            {
                bmList.set(i,Bitmap.createScaledBitmap(bmList.get(i), refWidth, refHeight, false));
            }
            invalidate();
            return true;
        }
    }

    //Change les valeurs de reference en fonction du coefficient de zoom
    void setRef(float imageScale) {
        //Zoom et déZoom par acoups
        for (int i = 0; i< maxIm; i++)
        {
            if(i/(float)maxIm < imageScale && imageScale < (i+1)/(float)maxIm)
            {
                refWidth = displayWidth/(maxIm-i);
                refHeight = maxHeight/(maxIm-i);
            }
        }
    }

    //Initialisation à la construction, car c'est la fonction de déZoom qui gére le rescale des images
    void init()
    {
        mScale = 1;
        setRef(mScale); //TO CHECK
        for (int i = min; i < max; i++)
        {
            bmList.set(i,Bitmap.createScaledBitmap(bmList.get(i), refWidth, refHeight, false));
        }
        invalidate();
        return;
    }

}

