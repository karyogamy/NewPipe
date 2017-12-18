package org.schabi.newpipe.pybridge;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.services.youtube.ItagItem;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.ArrayList;
import java.util.List;

import static org.schabi.newpipe.extractor.stream.StreamExtractor.NO_AGE_LIMIT;

public class GenericStreamInfo {

    @NonNull
    public static StreamInfo getInfo(final String url) throws JSONException, ExtractionException {
        final JSONObject doc = download(url);
        return getExtras(doc, getStreams(doc, getTemplate(doc)));
    }

    @NonNull
    private static JSONObject download(@NonNull final String url) throws ExtractionException {
        try {
            JSONObject json = new JSONObject();
            json.put("function", "dl");
            json.put("url", url);
            json.put("path", "/storage/emulated/0/Download/");
            json.put("skip", true);

            final JSONObject result = PyBridge.call(json);
            final String answer = result.getString("result");

            return new JSONObject(answer);
        } catch (Exception e) {
            throw new ExtractionException(e);
        }
    }

    private static StreamInfo getTemplate(@NonNull final JSONObject doc) throws JSONException {
        final int serviceId = Integer.MIN_VALUE;
        final String url = doc.getString("webpage_url");

        final StreamType type;
        if (doc.has("live") && doc.getString("live") != null) {
            type = StreamType.LIVE_STREAM;
        } else {
            type = StreamType.VIDEO_STREAM;
        }

        final String id = doc.getString("display_id");
        final String name = doc.getString("title");

        final int ageLimit;
        if (doc.has("age_limit") && doc.getInt("age_limit") > 0) {
            ageLimit = doc.getInt("age_limit");
        } else {
            ageLimit = NO_AGE_LIMIT;
        }

        return new StreamInfo(serviceId, url, type, id, name, ageLimit);
    }

    @NonNull
    private static StreamInfo getStreams(@NonNull final JSONObject doc,
                                         @NonNull final StreamInfo template) throws JSONException, ExtractionException {
        final JSONArray formats = doc.getJSONArray("formats");

        List<AudioStream> audioStreams = new ArrayList<>();
        List<VideoStream> videoStreams = new ArrayList<>();
        List<VideoStream> videoOnlyStreams = new ArrayList<>();
        List<String> nonHttpsProtocolUrls = new ArrayList<>();

        for (int i = 0; i < formats.length(); i++) {
            final JSONObject format = formats.getJSONObject(i);

            // Filter out hls or dash videos
            if (!format.getString("protocol").equals("https") && format.has("manifest_url")) {
                if (format.has("manifest_url")) {
                    nonHttpsProtocolUrls.add(format.getString("manifest_url"));
                }
                continue;
            }

            final String url = format.getString("url");

            final String ext = format.getString("ext");
            if (!isMediaFormatUsable(ext)) continue;
            final MediaFormat mediaFormat = getFormatBySuffix(ext);

            final String acodec = format.optString("acodec");
            final String vcodec = format.optString("vcodec");
            // These two props do not appear all the time
            // DO NOT treat them as 'none' if they don't appear
            final boolean isAudioOnly = vcodec.equals("none");
            final boolean isVideoOnly = acodec.equals("none");

            if (isAudioOnly) {
                final int averageBitrate = format.optInt("abr", -1);
                audioStreams.add(new AudioStream(url, mediaFormat, averageBitrate));
                continue;
            }

            final int itag = format.optInt("format_id", -1);
            final String formatId = format.getString("format_id");
            final String resolution;
            if (itag != -1 && ItagItem.isSupported(itag)) {
                resolution = ItagItem.getItag(itag).resolutionString;
            } else {
                resolution = formatId;
            }

            if (isVideoOnly) {
                videoOnlyStreams.add(new VideoStream(url, mediaFormat, resolution, true));
            } else {
                videoStreams.add(new VideoStream(url, mediaFormat, resolution, false));
            }
        }

        if (videoStreams.isEmpty() && audioStreams.isEmpty()) {
            throw new ExtractionException("Could not get any stream. See error variable to get further details.");
        }

        template.setVideoStreams(videoStreams);
        template.setVideoOnlyStreams(videoOnlyStreams);
        template.setAudioStreams(audioStreams);

        return template;
    }

    private static boolean isMediaFormatUsable(final String suffix) {
        for (MediaFormat vf: MediaFormat.values()) {
            if (vf.suffix.equals(suffix)) return true;
        }
        return false;
    }

    @NonNull
    private static MediaFormat getFormatBySuffix(final String suffix) throws ParsingException {
        for (MediaFormat vf: MediaFormat.values()) {
            if (vf.suffix.equals(suffix)) return vf;
        }
        throw new ParsingException("MediaFormat not available for suffix: [" + suffix + "]");
    }

    @NonNull
    private static StreamInfo getExtras(@NonNull final JSONObject doc,
                                        @NonNull final StreamInfo template) {
        try {
            template.setThumbnailUrl(doc.getString("thumbnail"));
        } catch (Exception e) {
            template.addError(e);
        }
        try {
            template.setDuration(doc.getInt("duration"));
        } catch (Exception e) {
            template.addError(e);
        }
        try {
            template.setUploaderName(doc.getString("uploader"));
        } catch (Exception e) {
            template.addError(e);
        }
        try {
            template.setUploaderUrl(doc.getString("uploader_url"));
        } catch (Exception e) {
            template.addError(e);
        }
        try {
            template.setDescription(doc.getString("description"));
        } catch (Exception e) {
            template.addError(e);
        }
        try {
            template.setViewCount(doc.getInt("view_count"));
        } catch (Exception e) {
            template.addError(e);
        }
        try {
            template.setUploadDate(doc.getString("upload_date_fail"));
        } catch (Exception e) {
            template.addError(e);
        }
        try {
            template.setUploaderAvatarUrl(doc.getString("uploader_thumbnail"));
        } catch (Exception e) {
            template.addError(e);
        }
        try {
            template.setStartPosition(doc.optInt("start_time", 0));
        } catch (Exception e) {
            template.addError(e);
        }
        try {
            template.setLikeCount(doc.getInt("like_count"));
        } catch (Exception e) {
            template.addError(e);
        }
        try {
            template.setDislikeCount(doc.getInt("dislike_count"));
        } catch (Exception e) {
            template.addError(e);
        }

        return template;
    }

}
