package io.github.lrsdev.dogbeaches;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;

import io.github.lrsdev.dogbeaches.R;
import io.github.lrsdev.dogbeaches.contentprovider.DogBeachesContract;

/**
 * Created by samuel on 8/07/15.
 */
public class LocationRecyclerFragment extends Fragment
{
    private final static String TAG = "LocationRecyclerFrag";
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Location mLastLocation;
    private Cursor mCursor;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_location_recycler, container, false);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.locationRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        queryLocations();
        mAdapter = new LocationRecyclerAdapter(mCursor, getActivity());
        mRecyclerView.setAdapter(mAdapter);

        return v;
    }

    /**
     * Query content provider for a location cursor. If location is available, order by location.
     */
    public void queryLocations()
    {
        mLastLocation = LocationManager.get(getActivity()).getLocation();
        String orderBy = "";
        if(mLastLocation != null)
        {
            orderBy = "abs(latitude - " + Double.toString(mLastLocation.getLatitude()) + ") " +
                    "+ abs(longitude - " + Double.toString(mLastLocation.getLongitude()) + ")";
        }
        // 'Manhatten' formula will order collection roughly by distance (but not perfectly).
        mCursor = getActivity().getContentResolver().query(DogBeachesContract.Locations.CONTENT_URI,
                DogBeachesContract.Locations.PROJECTION_ALL, null, null, orderBy);
    }

    @Override
    public void onDestroyView()
    {
        mCursor.close();
        super.onDestroyView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        getActivity().setTitle("Location List");
    }

    private class LocationRecyclerAdapter extends RecyclerView.Adapter<LocationRecyclerAdapter.ViewHolder>

    {
        private Cursor mLocationCursor;
        private Context mContext;
        private int mNameIndex;
        private int mIdIndex;
        private int mDogGuidelinesIndex;
        private int mLocalMediumImageIndex;
        private int mLatitudeIndex;
        private int mLongitudeIndex;

        public class ViewHolder extends RecyclerView.ViewHolder
        {
            public TextView titleTextView;
            public TextView guidelinesTextView;
            public TextView distanceTextView;
            public ImageView imageView;
            public Button moreInfoButton;
            public Location coordinates;
            public Integer locationId;

            public ViewHolder(View recyclerView)
            {
                super(recyclerView);
                titleTextView = (TextView) recyclerView.findViewById(R.id.locationCardTitleTextView);
                imageView = (ImageView) recyclerView.findViewById(R.id.locationCardImageView);
                guidelinesTextView = (TextView) recyclerView.findViewById(R.id.locationCardDogGuidelines);
                moreInfoButton = (Button) recyclerView.findViewById(R.id.locationCardMoreInfoButton);
                distanceTextView = (TextView) recyclerView.findViewById(R.id.locationCardDistanceTextView);

                moreInfoButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Intent i = new Intent(mContext, LocationActivity.class);
                        i.putExtra(LocationActivity.KEY_LOCATION_ID, locationId);
                        startActivity(i);
                    }
                });
            }
        }

        public LocationRecyclerAdapter(Cursor locationCursor, Context context)
        {
            mLocationCursor = locationCursor;
            mContext = context;
            mIdIndex = locationCursor.getColumnIndexOrThrow(DogBeachesContract.Locations.COLUMN_ID);
            mNameIndex = locationCursor.getColumnIndexOrThrow(DogBeachesContract.Locations.COLUMN_NAME);
            mDogGuidelinesIndex = locationCursor.getColumnIndexOrThrow(DogBeachesContract.Locations.COLUMN_DOG_GUIDELINES);
            mLocalMediumImageIndex = locationCursor.getColumnIndexOrThrow(DogBeachesContract.Locations.COLUMN_IMAGE);
            mLatitudeIndex = locationCursor.getColumnIndexOrThrow(DogBeachesContract.Locations.COLUMN_LATITUDE);
            mLongitudeIndex = locationCursor.getColumnIndexOrThrow(DogBeachesContract.Locations.COLUMN_LONGITUDE);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position)
        {
            mLocationCursor.moveToPosition(position);
            holder.locationId = mLocationCursor.getInt(mIdIndex);
            holder.titleTextView.setText(mLocationCursor.getString(mNameIndex));
            holder.guidelinesTextView.setText(mLocationCursor.getString(mDogGuidelinesIndex));
            File f = new File(mLocationCursor.getString(mLocalMediumImageIndex));
            Picasso.with(mContext).load(f).into(holder.imageView);

            holder.coordinates = new Location("");
            holder.coordinates.setLatitude(mLocationCursor.getDouble(mLatitudeIndex));
            holder.coordinates.setLongitude(mLocationCursor.getDouble(mLongitudeIndex));

            if (mLastLocation != null)
            {
                Float distance = mLastLocation.distanceTo(holder.coordinates);
                String kms = String.format("%.2f", (distance/1000)) + " Kms away";
                holder.distanceTextView.setText(kms);
            }
        }

        @Override
        public LocationRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_card,
                    parent, false);

            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public int getItemCount()
        {
            return mLocationCursor.getCount();
        }
    }
}