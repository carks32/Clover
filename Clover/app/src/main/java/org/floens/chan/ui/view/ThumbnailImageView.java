/*
 * Clover - 4chan browser https://github.com/Floens/Clover/
 * Copyright (C) 2014  Floens
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.floens.chan.ui.view;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.koushikdutta.ion.Response;

import org.floens.chan.ChanApplication;
import org.floens.chan.R;
import org.floens.chan.utils.Logger;
import org.floens.chan.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CancellationException;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class ThumbnailImageView extends LoadView implements View.OnClickListener {
    private static final String TAG = "ThumbnailImageView";

    private ThumbnailImageViewCallback callback;

    /**
     * Max amount to scale the image inside the view
     */
    private final float maxScale = 3f;

    private boolean thumbnailNeeded = true;

    private Request<?> imageRequest;
    private Future<?> ionRequest;
    private VideoView videoView;
    private GifDrawable gifDrawable;

    public ThumbnailImageView(Context context) {
        super(context);
        init();
    }

    public ThumbnailImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThumbnailImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOnClickListener(this);
    }

    public void setCallback(ThumbnailImageViewCallback callback) {
        this.callback = callback;
    }


    public void setThumbnail(String thumbnailUrl) {
        if (getWidth() == 0 || getHeight() == 0) {
            Logger.e(TAG, "getWidth() or getHeight() returned 0, not loading");
            return;
        }

        // Also use volley for the thumbnails
        ChanApplication.getVolleyImageLoader().get(thumbnailUrl, new com.android.volley.toolbox.ImageLoader.ImageListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onError();
            }

            @Override
            public void onResponse(ImageContainer response, boolean isImmediate) {
                if (response.getBitmap() != null && thumbnailNeeded) {
                    ImageView thumbnail = new ImageView(getContext());
                    thumbnail.setImageBitmap(response.getBitmap());
                    thumbnail.setLayoutParams(Utils.MATCH_PARAMS);
                    setView(thumbnail, false);
                }
            }
        }, getWidth(), getHeight());
    }

    public void setBigImage(String imageUrl) {
        if (getWidth() == 0 || getHeight() == 0) {
            Logger.e(TAG, "getWidth() or getHeight() returned 0, not loading");
            return;
        }

        callback.setProgress(true);

        File file = ChanApplication.getFileCache().get(imageUrl);
        if (file.exists()) {
            onBigImage(file);
        } else {
            ionRequest = Ion.with(getContext())
                    .load(imageUrl)
                    .progress(new ProgressCallback() {
                        @Override
                        public void onProgress(final long downloaded, final long total) {
                            Utils.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callback.setLinearProgress(downloaded, total, false);
                                }
                            });
                        }
                    })
                    .write(file)
                    .withResponse()
                    .setCallback(new FutureCallback<Response<File>>() {
                        @Override
                        public void onCompleted(Exception e, Response<File> result) {
                            if (result != null && result.getHeaders() != null && result.getHeaders().getResponseCode() / 100 != 2) {
                                if (result.getResult() != null) {
                                    ChanApplication.getFileCache().delete(result.getResult());
                                }
                                onNotFoundError();
                                return;
                            }

                            if (e != null && !(e instanceof CancellationException)) {
                                e.printStackTrace();
                                if (result != null && result.getResult() != null) {
                                    ChanApplication.getFileCache().delete(result.getResult());
                                }
                                onError();
                                return;
                            }

                            if (result != null && result.getResult() != null) {
                                ChanApplication.getFileCache().put(result.getResult());
                                onBigImage(result.getResult());
                            }
                        }
                    });
        }
    }

    private void onBigImage(File file) {
        SubsamplingScaleImageView image = new SubsamplingScaleImageView(getContext());
        image.setImageFile(file.getAbsolutePath());
        image.setOnClickListener(this);

        setView(image, false);
        callback.setProgress(false);
        callback.setLinearProgress(0, 0, true);
        thumbnailNeeded = false;
    }

    public void setGif(String gifUrl) {
        if (getWidth() == 0 || getHeight() == 0) {
            Logger.e(TAG, "getWidth() or getHeight() returned 0, not loading");
            return;
        }

        callback.setProgress(true);

        File file = ChanApplication.getFileCache().get(gifUrl);
        if (file.exists()) {
            onGif(file);
        } else {
            ionRequest = Ion.with(getContext())
                    .load(gifUrl)
                    .progress(new ProgressCallback() {
                        @Override
                        public void onProgress(final long downloaded, final long total) {
                            Utils.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callback.setLinearProgress(downloaded, total, false);
                                }
                            });
                        }
                    })
                    .write(file)
                    .withResponse()
                    .setCallback(new FutureCallback<Response<File>>() {
                        @Override
                        public void onCompleted(Exception e, Response<File> result) {
                            if (result != null && result.getHeaders() != null && result.getHeaders().getResponseCode() / 100 != 2) {
                                if (result.getResult() != null) {
                                    ChanApplication.getFileCache().delete(result.getResult());
                                }
                                onNotFoundError();
                                return;
                            }

                            if (e != null && !(e instanceof CancellationException)) {
                                e.printStackTrace();
                                if (result != null && result.getResult() != null) {
                                    ChanApplication.getFileCache().delete(result.getResult());
                                }
                                onError();
                                return;
                            }

                            if (result != null && result.getResult() != null) {
                                ChanApplication.getFileCache().put(result.getResult());
                                onGif(result.getResult());
                            }
                        }
                    });
        }
    }

    private void onGif(File file) {
        GifDrawable drawable;
        try {
            drawable = new GifDrawable(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            onError();
            return;
        }

        GifImageView view = new GifImageView(getContext());
        view.setImageDrawable(drawable);
        view.setLayoutParams(Utils.MATCH_PARAMS);
        setView(view, false);

        callback.setProgress(false);
        callback.setLinearProgress(0, 0, true);
        thumbnailNeeded = false;
    }

    public void setVideo(String videoUrl) {
        callback.setProgress(true);

        File file = ChanApplication.getFileCache().get(videoUrl);
        if (file.exists()) {
            onVideo(file);
        } else {
            ionRequest = Ion.with(getContext())
                    .load(videoUrl)
                    .progress(new ProgressCallback() {
                        @Override
                        public void onProgress(final long downloaded, final long total) {
                            Utils.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callback.setLinearProgress(downloaded, total, false);
                                }
                            });
                        }
                    })
                    .write(file)
                    .withResponse()
                    .setCallback(new FutureCallback<Response<File>>() {
                        @Override
                        public void onCompleted(Exception e, Response<File> result) {
                            if (result != null && result.getHeaders() != null && result.getHeaders().getResponseCode() / 100 != 2) {
                                if (result.getResult() != null) {
                                    ChanApplication.getFileCache().delete(result.getResult());
                                }
                                onNotFoundError();
                                return;
                            }

                            if (e != null && !(e instanceof CancellationException)) {
                                e.printStackTrace();
                                if (result != null && result.getResult() != null) {
                                    ChanApplication.getFileCache().delete(result.getResult());
                                }
                                onError();
                                return;
                            }

                            if (result != null && result.getResult() != null) {
                                ChanApplication.getFileCache().put(result.getResult());
                                onVideo(result.getResult());
                            }
                        }
                    });
        }
    }

    private void onVideo(File file) {
        videoView = new VideoView(getContext());
        videoView.setZOrderOnTop(true);
        videoView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        videoView.setLayoutParams(Utils.MATCH_PARAMS);
        LayoutParams par = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        par.gravity = Gravity.CENTER;
        videoView.setLayoutParams(par);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
                callback.onVideoLoaded();
            }
        });

        videoView.setVideoPath(file.getAbsolutePath());

        setView(videoView, false);
        callback.setProgress(false);
        callback.setLinearProgress(0, 0, true);
        thumbnailNeeded = false;

        videoView.start();
    }

    @Override
    public void setView(View view, boolean animation) {
        super.setView(view, animation && !thumbnailNeeded);
    }

    public VideoView getVideoView() {
        return videoView;
    }

    public void onError() {
        Toast.makeText(getContext(), R.string.image_preview_failed, Toast.LENGTH_LONG).show();
        callback.setProgress(false);
    }

    public void onNotFoundError() {
        Toast.makeText(getContext(), R.string.image_not_found, Toast.LENGTH_LONG).show();
        callback.setProgress(false);
    }

    public void cancelLoad() {
        if (imageRequest != null) {
            imageRequest.cancel();
            imageRequest = null;
        }

        if (ionRequest != null) {
            ionRequest.cancel(true);
        }
    }

    @Override
    public void onClick(View v) {
        callback.onTap();
    }

    public static interface ThumbnailImageViewCallback {
        public void onTap();

        public void setProgress(boolean progress);

        public void setLinearProgress(long current, long total, boolean done);

        public void onVideoLoaded();
    }
}
