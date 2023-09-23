package com.trialapp;

import android.net.Uri;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.annotations.ReactPropGroup;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.pspdfkit.configuration.PdfConfiguration;
import com.pspdfkit.configuration.page.PageScrollDirection;
import com.pspdfkit.document.download.DownloadJob;
import com.pspdfkit.document.download.DownloadRequest;
import com.pspdfkit.document.download.Progress;
import com.pspdfkit.ui.PdfFragment;

import java.io.File;
import java.util.Map;

public class PdfViewManager extends ViewGroupManager<FrameLayout> {

    public static final String REACT_CLASS = "PdfViewManager";
    public final int COMMAND_CREATE = 1;
    private int propWidth;
    private int propHeight;

    private final ReactApplicationContext reactContext;

    private int reactNativeViewId = -1;
    private String url;

    public PdfViewManager(ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    /**
     * Return a FrameLayout which will later hold the Fragment
     */
    @Override
    public FrameLayout createViewInstance(ThemedReactContext reactContext) {
        return new FrameLayout(reactContext);
    }

    /**
     * Map the "create" command to an integer
     */
    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of("create", COMMAND_CREATE);
    }

    /**
     * Register the "onLongPressDocument" event
     */
    @Nullable
    @Override
    public Map<String, Object> getExportedCustomBubblingEventTypeConstants() {
        return MapBuilder.<String, Object>builder().put(
                "onLongPressDocument",
                MapBuilder.of(
                        "phasedRegistrationNames",
                        MapBuilder.of("bubbled", "onChange")
                )
        ).build();
    }

    /**
     * Handle "create" command (called from JS) and call createFragment method
     */
    @Override
    public void receiveCommand(
            @NonNull FrameLayout root,
            String commandId,
            @Nullable ReadableArray args
    ) {
        super.receiveCommand(root, commandId, args);
        reactNativeViewId = args.getInt(0);
        int commandIdInt = Integer.parseInt(commandId);

        switch (commandIdInt) {
            case COMMAND_CREATE:
                createFragment(root, reactNativeViewId);
                break;
            default: {
            }
        }
    }

    /**
     * Set width and height of the view
     */
    @ReactPropGroup(names = {"width", "height"}, customType = "Style")
    public void setStyle(FrameLayout view, int index, Integer value) {
        if (index == 0) {
            propWidth = value;
        }

        if (index == 1) {
            propHeight = value;
        }
    }

    /**
     * Set/update the document URL
     */
    @ReactProp(name = "documentURL")
    public void documentURL(FrameLayout view, String url) {
        this.url = url;

        if (reactNativeViewId != -1) {
            updatePdfUrl();
        }
    }

    /**
     * Update the PDF URL, show a progress bar while downloading, or show an error message
     */
    private void updatePdfUrl() {
        FragmentActivity activity = (FragmentActivity) reactContext.getCurrentActivity();

        DownloadingFragment downloadingFragment = new DownloadingFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(reactNativeViewId, downloadingFragment, String.valueOf(reactNativeViewId))
                .commit();


        final DownloadRequest request = new DownloadRequest.Builder(reactContext)
                .uri(url)
                .build();

        final DownloadJob job = DownloadJob.startDownload(request);
        job.setProgressListener(
                new DownloadJob.ProgressListener() {
                    @Override
                    public void onProgress(@NonNull Progress progress) {
                        View view = downloadingFragment.getView();
                        if (view != null) {
                            ((LinearProgressIndicator) view.findViewById(R.id.pb_downloading)).setProgress((int) (progress.bytesReceived));
                            ((LinearProgressIndicator) view.findViewById(R.id.pb_downloading)).setMax((int) progress.totalBytes);
                        }
                    }

                    @Override
                    public void onComplete(@NonNull File output) {
                        final PdfConfiguration configuration = new PdfConfiguration.Builder()
                                .scrollDirection(PageScrollDirection.HORIZONTAL)
                                .build();

                        final PdfFragment pdfFragment = PdfFragment.newInstance(Uri.fromFile(output), configuration);
                        pdfFragment.setOnDocumentLongPressListener((pdfDocument, i, motionEvent, pointF, annotation) -> {
                            if (pointF == null) {
                                return false;
                            }

                            WritableMap event = Arguments.createMap();
                            event.putInt("page", i);
                            event.putDouble("x", pointF.x);
                            event.putDouble("y", pointF.y);
                            reactContext
                                    .getJSModule(RCTEventEmitter.class)
                                    .receiveEvent(reactNativeViewId, "onLongPressDocument", event);
                            return true;
                        });

                        FragmentActivity activity = (FragmentActivity) reactContext.getCurrentActivity();
                        activity.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(reactNativeViewId, pdfFragment, String.valueOf(reactNativeViewId))
                                .commit();
                    }

                    @Override
                    public void onError(@NonNull Throwable exception) {
                        Log.e(REACT_CLASS, "Error downloading document", exception);
                        ErrorFragment errorFragment = ErrorFragment.newInstance(exception.getMessage());
                        FragmentActivity activity = (FragmentActivity) reactContext.getCurrentActivity();
                        activity.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(reactNativeViewId, errorFragment, String.valueOf(reactNativeViewId))
                                .commit();
                    }
                });
    }

    /**
     * Replace your React Native view with a custom fragment
     */
    public void createFragment(FrameLayout root, int reactNativeViewId) {
        ViewGroup parentView = (ViewGroup) root.findViewById(reactNativeViewId);
        setupLayout(parentView);

        if (url != null) {
            updatePdfUrl();
            return;
        }

        FragmentActivity activity = (FragmentActivity) reactContext.getCurrentActivity();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(reactNativeViewId, new BlankFragment(), String.valueOf(reactNativeViewId))
                .commit();
    }

    public void setupLayout(View view) {
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                manuallyLayoutChildren(view);
                view.getViewTreeObserver().dispatchOnGlobalLayout();
                Choreographer.getInstance().postFrameCallback(this);
            }
        });
    }

    /**
     * Layout all children properly
     */
    public void manuallyLayoutChildren(View view) {
        // propWidth and propHeight coming from react-native props
        int width = propWidth;
        int height = propHeight;

        view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));

        view.layout(0, 0, width, height);
    }
}