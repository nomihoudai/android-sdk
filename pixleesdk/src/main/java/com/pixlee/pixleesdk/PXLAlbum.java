package com.pixlee.pixleesdk;

import android.content.Context;
import android.util.Log;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/***
 * Represents a Pixlee album. Constructs appropriate API calls to fetch the desired set of photos.
 * Specify all sort/filter/etc. parameters before calling 'loadNextPageOfPhotos'. Basic usage:
 * --construct an album object
 * --specify photos per page, sort options, and filter options
 * --call 'loadNextPageOfPhotos'
 */
public class PXLAlbum implements RequestCallbacks {
    private static final String TAG = "PXLAlbum";
    public static final int DefaultPerPage = 20;

    public String id = null;
    private int page;
    private int perPage;
    private boolean hasMore;
    private int lastPageLoaded;
    private ArrayList<PXLPhoto> photos;
    private PXLAlbumFilterOptions filterOptions;
    private PXLAlbumSortOptions sortOptions;
    private HashMap<Integer, Boolean> pagesLoading;
    private RequestHandlers handlers;
    private Context context;

    /***
     * Callback for a successful call to the api.  Parses the response and converts the json data
     * to PXLPhoto objects.
     * @param response - JSONObject from the request
     */
    @Override
    public void JsonReceived(JSONObject response) {
        try {
            this.page = response.getInt("page");
            this.perPage = response.getInt(("per_page"));
            this.hasMore = response.getBoolean(("next"));
            //add placeholders for photos if they haven't been loaded yet
            if (this.photos.size() < (this.page - 1) * this.perPage) {
                for (int i = this.photos.size(); i < (this.page - 1) * this.perPage; i++) {
                    this.photos.add(null);
                }
            }
            this.photos.addAll(this.photos.size(), PXLPhoto.fromJsonArray(response.getJSONArray("data"), this));
            this.lastPageLoaded = Math.max(this.page, this.lastPageLoaded);

            //handlers set when making the original 'loadNextPageOfPhotos' call
            if (handlers != null) {
                handlers.DataLoadedHandler(this.photos);
            }

            // fire opened widget analytics event
            if(this.page == 1){
                openedWidget();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /***
     * Callback for errors that occur during the api request
     * @param error - error from volley
     */
    @Override
    public void ErrorResponse(VolleyError error) {
        if (handlers != null) {
            handlers.DataLoadFailedHandler(error.toString());
        }
    }

    /***
     * Interface for callbacks for loadNextPageOfPhotos
     */
    public interface RequestHandlers {
        void DataLoadedHandler(ArrayList<PXLPhoto> photos);
        void DataLoadFailedHandler(String error);
    }

    /***
     * Constructor requires the album id and context, which will be passed along to the PXLClient
     * for volley configuration.
     * @param id - album id
     * @param context - context which will be used for volley configuration
     */
    public PXLAlbum(String id, Context context) {
        this.id = id;
        this.page = 0;
        this.perPage = DefaultPerPage;
        this.hasMore = true;
        this.lastPageLoaded = 0;
        this.photos = new ArrayList<>();
        this.pagesLoading = new HashMap<>();
        this.context = context;
    }

    /***
     * Requests the next page of photos from the Pixlee album. Make sure to set perPage,
     * sort order, and filter options before calling.
     * @param handlers - called upon success/failure of the request
     * @return true if the request was attempted, false if aborted before the attempt was made
     */
    public boolean loadNextPageOfPhotos(final RequestHandlers handlers) {
        if (id == null) {
            Log.w(TAG, "No album id specified");
            return false;
        }
        if (this.hasMore) {
            int desiredPage = this.lastPageLoaded + 1;
            if (pagesLoading.get(desiredPage) != null && pagesLoading.get(desiredPage)) {
                Log.d(TAG, String.format("page %s already loading", desiredPage));
                return false;
            }
            PXLClient pxlClient = PXLClient.getInstance(context);
            String requestPath = String.format("albums/%s/photos", this.id);
            this.pagesLoading.put(desiredPage, true);
            this.handlers = handlers;
            pxlClient.makeCall(requestPath, getRequestParams(desiredPage), this);
        }

        return true;
    }

    /***
     * Sets the amount of photos fetched per call of 'loadNextPageOfPhotos'.  Will purge previously
     * fetched photos. Call 'loadNextPageOfPhotos' after setting.
     * @param perPage - number of photos per page
     */
    public void setPerPage(int perPage) {
        this.perPage = perPage;
        this.resetState();
    }

    /***
     * Sets the filter options for the album. Will purge previously fetched photos. Call
     * 'loadNextPageOfPhotos' after setting.
     * @param filterOptions
     */
    public void setFilterOptions(PXLAlbumFilterOptions filterOptions) {
        this.filterOptions = filterOptions;
        this.resetState();
    }

    /***
     * Sets the sort options for the album. Will purge previously fetched photos. Call
     * 'loadNextPageOfPhotos' after setting.
     * @param sortOptions
     */
    public void setSortOptions(PXLAlbumSortOptions sortOptions) {
        this.sortOptions = sortOptions;
        this.resetState();
    }

    private void resetState() {
        this.photos.clear();
        this.lastPageLoaded = 0;
        this.hasMore = true;
        this.pagesLoading.clear();
    }

    private HashMap<String, Object> getRequestParams(int desiredPage) {
        HashMap<String, Object> paramMap = new HashMap<>();
        if (filterOptions != null) {
            paramMap.put(PXLClient.KeyFilters, filterOptions.toParamString());
        }
        if (sortOptions != null) {
            paramMap.put(PXLClient.KeySort, sortOptions.toParamString());
        }
        paramMap.put(PXLClient.KeyPerPage, perPage);
        paramMap.put(PXLClient.KeyPage, desiredPage);
        return paramMap;
    }

    public boolean openedWidget() {
        PXLClient pxlClient = PXLClient.getInstance(context);
        JSONObject body = new JSONObject();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < this.photos.size(); i++) {
            try {
                stringBuilder.append(this.photos.get(i).id);
                stringBuilder.append(",");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try{
            body.put("album_id", this.id);
            body.put("per_page", this.perPage);
            body.put("page", this.page);
            body.put("photos", stringBuilder.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        pxlClient.makeAnalyticsCall("events/openedWidget", body);
        return true;
    }
}
