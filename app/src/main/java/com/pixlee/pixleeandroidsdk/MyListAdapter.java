package com.pixlee.pixleeandroidsdk;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.pixlee.pixleesdk.PXLClient;
import com.pixlee.pixleesdk.PXLPhoto;
import com.pixlee.pixleesdk.PXLPhotoSize;

import java.util.ArrayList;

/**
 * Created by andy on 4/4/17.
 */

class MyListAdapter extends RecyclerView.Adapter<MyViewHolder> {
    private ArrayList<PXLPhoto> galleryList;
    private Context context;
    private ImageLoader imageLoader;

    public MyListAdapter(Context context, ArrayList<PXLPhoto> galleryList) {
        this.galleryList = galleryList;
        this.context = context;
        this.imageLoader = PXLClient.getInstance(context).getImageLoader();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row, viewGroup, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder viewHolder, int i) {
        PXLPhoto photo = galleryList.get(i);
        viewHolder.title.setText(photo.photoTitle);
        //viewHolder.description.setText("Sample text hello there");
        if (photo.thumbnailUrl == null) {
            Log.e("listadapter", "failed to get thumbnail url for photo " + photo.id);
        } else {
            Log.d("listadapter", String.format("setting url for photo %s: %s", i, photo.getUrlForSize(PXLPhotoSize.THUMBNAIL).toString()));
        }
        viewHolder.netImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        viewHolder.netImg.setImageUrl(photo.thumbnailUrl.toString(), imageLoader);
        //viewHolder.netImg.setImageResource((galleryList.get(i).thumbnailUrl.toString())));
    }

    @Override
    public int getItemCount() {
        return galleryList.size();
    }

    //private Context mContext;
    //private String[]  Title;
    //private int[] imge;
}
