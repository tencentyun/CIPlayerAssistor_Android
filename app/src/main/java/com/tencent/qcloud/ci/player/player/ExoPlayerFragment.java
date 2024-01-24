/*
 * Copyright (c) 2010-2020 Tencent Cloud. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.tencent.qcloud.ci.player.player;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.tencent.qcloud.ci.player.R;

/**
 * google exoplayer播放器页面
 */
public class ExoPlayerFragment extends BasePlayerFragment {
    private ExoPlayer player;
    public ExoPlayerFragment() {
        super(R.layout.fragment_exo);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PlayerView playerView = view.findViewById(R.id.video_view);
        player = new ExoPlayer.Builder(getActivity()).build();
        playerView.setPlayer(player);
        loadUrl();
    }

    /**
     * 加载视频url
     */
    private void loadUrl() {
        MediaItem mediaItem = MediaItem.fromUri(videoPlayerActivity.getUrl());
        HlsMediaSource mediaSource = new HlsMediaSource.Factory(dataType ->
                new DefaultHttpDataSource.Factory().createDataSource()).createMediaSource(mediaItem);
        player.prepare(mediaSource);
        player.play();
    }

    @Override
    public void onPause() {
        super.onPause();
        player.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.release();
        player = null;
    }
}
