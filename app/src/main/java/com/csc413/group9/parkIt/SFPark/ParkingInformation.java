package com.csc413.group9.parkIt.SFPark;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.text.format.DateUtils;

import com.csc413.group9.parkIt.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.Scanner;

/**
 * This class handles the parking information around the device's current location. This class will
 * load the data from the SFPark and display it on the Google map.
 *
 * Created by Su Khai Koh on 4/19/15.
 */
public class ParkingInformation {

    /**
     * The SFPark URL that contains the parking data this application needs.
     */
    private static final String SERVICE_URL = "http://api.sfpark.org/sfpark/rest/availabilityservice?radius=1000.0&response=json&version=1.0&pricing=yes";

    /**
     *SFSU URL parking lots containing parking data this application needs.
     */
    //private static final String google_URL = "https://docs.google.com/document/d/15QUYceBcLUVLk398dTuCuiHv98Nq32YT48pcYT-iUMg/edit";
    private static final String GPS_Path = "/GPS.txt";
    /**
     * content of SFSU Parking URL
     */
    private String fileContent;
    /**
     * A reference to the application's context.
     */
    private Context mContext;

    /**
     * A reference to the Google map.
     */
    private GoogleMap mMap;

    /**
     * The content of the SFPark URL.
     */
    private String uriContent;

    /**
     * A list of on-street parking locations.
     */
    private ArrayList<ParkingLocation> onStreetParkings;

    /**
     * A list of off-street parking locations (garage parking).
     */
    private ArrayList<ParkingLocation> offStreetParkings;

    /**
     * A list of on-street parking location icons, which are Polylines on the Google map.
     */
    private ArrayList<Polyline> onStreetParkingIcons;

    /**
     * A list of off-street parking location icons, which are Markers on the Google map.
     */
    private ArrayList<Marker> offStreetParkingIcons;

    /**
     *Flag for the SFSU Park data
     */
    private boolean sfsuParkDataReady = false;

    /**
     * Flag for the SFPark data readiness. Default to false (not ready).
     */
    private boolean sfParkDataReady = false;

    /**
     * Flag for whether the on-street parking has already been highlighted.
     */
    private boolean highlightedOnStreetParking;

    /**
     * Flag for whether the off-street parking (garage parking) has already been highlighted.
     */
    private boolean highlightedOffStreetParking;

    /**
     * Constructor that setup the class members and references.
     * @param context the context of the application
     * @param map the Google map
     */
    public ParkingInformation(Context context, GoogleMap map) {

        mContext = context;
        mMap = map;

        initializeArrays();

        getSFParkData();
    }

    /**
     *
     */
    private void getSFParkData(){
        LoadSFParkDataTask ls = new LoadSFParkDataTask();
        ls.execute();
    }
    /**
     *
     * @return
     */
    public boolean isSfSUParkDataReady(){
       return sfsuParkDataReady;
    }
    /**
     * Determine if the SFPark data is ready.
     * @return true if the SFPark data is ready, false otherwise
     */
    public boolean isSFParkDataReady() {
        return sfParkDataReady;
    }

    /**
     * Highlight on-street and off-street parking locations based on the given parameters.
     * @param drawOnStreet whether to draw on-street parking or not (true = draw, false otherwise)
     * @param drawOffStreet whether to draw off-street parking or not (true = draw, false otherwise)
     */
    public void highlightStreet(boolean drawOnStreet, boolean drawOffStreet) {

        if (sfParkDataReady && drawOnStreet && !highlightedOnStreetParking) {
            highlightOnStreetParking();
            highlightedOnStreetParking = true;
        } else if (sfParkDataReady && !drawOnStreet) {
            removeOnStreetHighlighted();
            highlightedOnStreetParking = false;
        }

        if (sfParkDataReady && drawOffStreet && !highlightedOffStreetParking) {
            highlightOffStreetParking();
            highlightedOffStreetParking = true;
        } else if (sfParkDataReady && !drawOffStreet) {
            removeOffStreetHighlighted();
            highlightedOffStreetParking = false;
        }

        if(sfsuParkDataReady && drawOnStreet && !highlightedOnStreetParking){
            highlightOnStreetParking();
            highlightedOnStreetParking = true;
        }else if (sfsuParkDataReady && !drawOnStreet){
            removeOnStreetHighlighted();
        }

        if(sfsuParkDataReady && drawOffStreet && !highlightedOffStreetParking){
            highlightOffStreetParking();
            highlightedOffStreetParking = true;
        }else if (sfsuParkDataReady && !drawOffStreet){
            removeOffStreetHighlighted();
            highlightedOffStreetParking = true;
        }
    }

    /**
     * Highlight on-street parking on the map by setting the icons to visible.
     */
    private void highlightOnStreetParking() {

        for (int i = 0; i < onStreetParkingIcons.size(); i++) {
            onStreetParkingIcons.get(i).setVisible(true);
        }
    }

    /**
     * Highlight the off-street parking (parking lot building) on the map by setting the icons
     * to visible.
     */
    private void highlightOffStreetParking() {

        for (int i = 0; i < offStreetParkingIcons.size(); i++) {
            offStreetParkingIcons.get(i).setVisible(true);
        }
    }

    /**
     * Remove the on-street parking highlight from the map by setting the icons to invisible.
     */
    private void removeOnStreetHighlighted() {

        for (int i = 0; i < onStreetParkingIcons.size(); i++) {
            onStreetParkingIcons.get(i).setVisible(false);
        }
    }

    /**
     * Remove the off-street parking highlight from the map by setting the icons to invisible.
     */
    private void removeOffStreetHighlighted() {

        for (int i = 0; i < offStreetParkingIcons.size(); i++) {
            offStreetParkingIcons.get(i).setVisible(false);
        }
    }

    /**
     * Initialize onStreetParkings, offStreetParkings, onStreetParkingIcons, and
     * offStreetParkingIcons arrays.
     */
    private void initializeArrays() {

        if (onStreetParkings == null)
            onStreetParkings = new ArrayList<>();
        else
            onStreetParkings.clear();

        if (offStreetParkings == null)
            offStreetParkings = new ArrayList<>();
        else
            offStreetParkings.clear();

        if (onStreetParkingIcons == null)
            onStreetParkingIcons = new ArrayList<>();
        else
            onStreetParkingIcons.clear();

        if (offStreetParkingIcons == null)
            offStreetParkingIcons = new ArrayList<>();
        else
            offStreetParkingIcons.clear();
    }

    /**
     *  LoadSFPark
     */
    private class LoadSFParkDataTask extends AsyncTask<Void, ParkingLocation, Void> {

        // Constants for HTTP request
        /**
         * Default wait time for  HTTP connection.
         */
        private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;

        /**
         * Default socket buffer size for HTTP connection.
         */
        private static final int SOCKET_BUFFER_SIZE = 8192;

        /**
         * The progress dialog that will be shown when fetching the data from SFPark.
         */
        private ProgressDialog progressDialog;

        /**
         * A date format with the format of HH:mm.
         */
        private DateFormat dateFormat = new SimpleDateFormat("HH:mm");

        /**
         * A reference to the current hour and minute
         */
        private Date currentTime;

        @Override
        protected void onPreExecute() {

            try {
                progressDialog = new ProgressDialog(mContext);
                progressDialog.setTitle("Please wait");
                progressDialog.setMessage("Displaying parking data ...");
                progressDialog.show();
                progressDialog.setCancelable(false);

                currentTime = dateFormat.parse(dateFormat.format(new Date(System.currentTimeMillis())));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {

            loadSFParkData();
            readSFParkData();
            loadSFSUParkData();
            readSFSUParkData();

            return null;
        }

        @Override
        protected void onProgressUpdate(ParkingLocation... locations) {

            if (locations[0].isOnStreet()) {
                initializeOnStreetParking(locations[0]);
            } else {
                initializeOffStreetParking(locations[0]);
            }
        }

        @Override
        protected void onPostExecute(Void v) {

            sfParkDataReady = true;
            sfsuParkDataReady = true;

            progressDialog.dismiss();
        }

        /**
         * Load SFSU Parking data from the GSP.txt.
         */
        private void loadSFSUParkData() {

            if (fileContent != null)
                return;

            try {
                fileContent = getFileContent(GPS_Path);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Read the SFSU Park data after it is loaded into the application.
         */
        private void readSFSUParkData() {

            try {
                // Don't do anything if there is nothing available in the SFSU google URL GPS.txt
                if (fileContent == null || fileContent.equals(""))
                    return;

                JSONObject fileObject = new JSONObject(fileContent);
                JSONArray jsonFile = fileObject.has("AVL") ? fileObject.getJSONArray("AVL") : null;

                if (jsonFile == null)
                    return;

                for (int i = 0; i < jsonFile.length(); i++) {
                    // Get the file value (a location JSON data) and parse it
                    JSONObject coordinate = jsonFile.getJSONObject(i);
                    ParkingLocation parkingLocation = new ParkingLocation(coordinate);

                    publishProgress(parkingLocation);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Load SFPark data from the SFPark URL.
         */
        private void loadSFParkData() {

            if (uriContent != null)
                return;

            try {
                uriContent = getURIContent(SERVICE_URL);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Read the SFPark data after it is loaded into the application.
         */
        private void readSFParkData() {

            try {
                // Don't do anything if there is nothing available in the SFPark URL
                if (uriContent == null || uriContent.equals(""))
                    return;

                JSONObject rootObject = new JSONObject(uriContent);
                JSONArray jsonAVL = rootObject.has("AVL") ? rootObject.getJSONArray("AVL") : null;

                if (jsonAVL == null)
                    return;

                for (int i = 0; i < jsonAVL.length(); i++) {
                    // Get the AVL value (a location JSON data) and parse it
                    JSONObject location = jsonAVL.getJSONObject(i);
                    ParkingLocation parkingLocation = new ParkingLocation(location);

                    publishProgress(parkingLocation);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Initialize the on-street parking location and display it on the map.
         * @param parkingLocation the on-street parking location to be initialized
         */
        private void initializeOnStreetParking(ParkingLocation parkingLocation) {

            onStreetParkings.add(parkingLocation);

            // If it is street cleaning time now, then don't show the line
            ParkingLocation.RateSchedule[] rateSchedules = parkingLocation.getRateSchedule();
            for (int i = 0; i < rateSchedules.length; i++) {
                ParkingLocation.RateSchedule rateSchedule = rateSchedules[i];

                if (rateSchedule.getRateRestriction().equals(ParkingLocation.VALUE_STREET_SWEEP)) {
                    if (!passedTime(rateSchedule.getEndTime())) {
                        return;
                    } else {
                        break;
                    }
                }
            }

            Location[] locations = parkingLocation.getLocation();

            // If there are 2 points, then draw a line from point1 to point2
            if (locations.length == 2) {
                // Draw a line
                PolylineOptions lineOptions = new PolylineOptions()
                        .add(new LatLng(locations[0].getLatitude(), locations[0].getLongitude()))
                        .add(new LatLng(locations[1].getLatitude(), locations[1].getLongitude()))
                        .color(Color.GREEN)
                        .width(7f);

                Polyline polyline = mMap.addPolyline(lineOptions);
                onStreetParkingIcons.add(polyline);
            }
        }

        /**
         * Initialize off-street parking location (parking garage) and display it on the map.
         * @param parkingLocation the off-street parking location to be initialized
         */
        private void initializeOffStreetParking(ParkingLocation parkingLocation) {

            offStreetParkings.add(parkingLocation);

            String name = parkingLocation.getName();
            String description = parkingLocation.getDescription();
            ParkingLocation.RateSchedule[] rateSchedules = parkingLocation.getRateSchedule();
            StringBuilder snippet = new StringBuilder();

            if (rateSchedules != null) {
                for (int i = 0; i < rateSchedules.length; i++) {

                    ParkingLocation.RateSchedule rate = rateSchedules[i];

                    if (!rate.getBeginTime().equals("")) {
                        snippet.append(rate.getBeginTime() + "-" + rate.getEndTime() + " : $" +
                                rate.getRate() + " " + rate.getRateQualifier() + "\n");
                    }
                }
            }

            Location[] locations = parkingLocation.getLocation();

            MarkerOptions markerOptions = new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_offstreet_parking))
                    .position(new LatLng(locations[0].getLatitude(), locations[0].getLongitude()))
                    .title(name)
                    .snippet(description + "%" + snippet);

            offStreetParkingIcons.add(mMap.addMarker(markerOptions));
        }

        /**
         * Check whether the given time has passed the current time. The given time must be in the
         * format: HH:mm AM/PM. Return true if the given time has passed the current time, false
         * otherwise.
         * @param givenTime the given time in the format HH:mm AM/PM
         * @return true if the given time has passed the current time, false otherwise
         */
        private boolean passedTime(String givenTime) {

            // Parse out the given time from string into integer
            // Assuming the given time has the format: HH:mm AM
            String[] time = givenTime.split("\\s+");

            // Assume it is available to park if the given time is in invalid format
            if (time == null || time.length != 2) {
                return true;
            }

            String hourMinute = time[0];        // Should have the format: HH:mm
            String period = time[1];            // Should contain AM or PM

            String[] hm = hourMinute.split(":");

            if (hm == null || hm.length != 2) {
                return true;
            }

            int hour = Integer.parseInt(hm[0]);

            String hhmm = period.equalsIgnoreCase("PM") ?
                    (Integer.toString(hour + 12) + ":" + hm[1]) :
                    ("0" + hour + ":" + hm[1]);

            try {
                Date givenDate = dateFormat.parse(hhmm);

                if (currentTime.before(givenDate)) {
                    return false;
                } else {
                    return true;
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return true;
        }

        /**
         * Get the URI content, which is the data from the SFPark.
         * @param uri the SFSU Park URI
         * @return a string that contains the content (SFPark data) from the SFPark website
         * @throws Exception any error when doing HTTP request
         */
        private String getFileContent(String file) throws Exception {

            AssetManager assetManager = mContext.getResources().getAssets();
            InputStream inputStream = null;
            String line = "";

            //mContext.getResources().openRawResource(R.id.);
            try {
                inputStream = assetManager.open("GPS.txt");
                System.out.printf("File Located");

            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (inputStream != null){
                System.out.println("Here!!!!!!!!!!!!!");
                BufferedReader reader = new BufferedReader( new InputStreamReader(inputStream));
                line = reader.readLine();
            }
            return line;
        }

        /**
         * Get the URI content, which is the data from the SFPark.
         * @param uri the SFPark URI
         * @return a string that contains the content (SFPark data) from the SFPark website
         * @throws Exception any error when doing HTTP request
         */
        private String getURIContent(String uri) throws Exception {

            try {
                HttpGet request = new HttpGet();
                request.setURI(new URI(uri));
                request.addHeader("Accept-Encoding", "gzip");

                final HttpParams params = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(params, 30 * SECOND_IN_MILLIS);
                HttpConnectionParams.setSoTimeout(params, 30 * SECOND_IN_MILLIS);
                HttpConnectionParams.setSocketBufferSize(params, SOCKET_BUFFER_SIZE);

                final DefaultHttpClient client = new DefaultHttpClient(params);

                client.addResponseInterceptor(new HttpResponseInterceptor() {

                    @Override
                    public void process(HttpResponse response, HttpContext context) {

                        final HttpEntity entity = response.getEntity();
                        final Header encoding = entity.getContentEncoding();

                        if (encoding != null) {
                            for (HeaderElement element : encoding.getElements()) {
                                if (element.getName().equalsIgnoreCase("gzip")) {
                                    response.setEntity(new InflatingEntity(response.getEntity()));
                                    break;
                                }
                            }
                        }
                    }
                });

                return client.execute(request, new BasicResponseHandler());

            } finally {
                // No clean up code is needed
            }
        }

        /**
         * Base class for wrapping entities. Keeps a wrappedEntity and delegates all calls to it.
         */
        private class InflatingEntity extends HttpEntityWrapper {

            public InflatingEntity(HttpEntity wrapped) {
                super(wrapped);
            }

            @Override
            public InputStream getContent() throws IOException {
                return new GZIPInputStream(wrappedEntity.getContent());
            }

            @Override
            public long getContentLength() {
                return -1;
            }
        }
    }
}
