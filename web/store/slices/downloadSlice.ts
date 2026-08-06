import { createAsyncThunk, createSlice, type PayloadAction } from "@reduxjs/toolkit";
import { APP } from "@/lib/app";
import { logger } from "@/lib/logger";
import type { DownloadState } from "@/types";

const initialState: DownloadState = {
  status: "idle",
  fileName: APP.downloadFileName,
  sizeBytes: APP.sizeBytes,
  version: APP.version,
  error: null,
  startedAt: null,
};

export const downloadApk = createAsyncThunk<
  { sizeBytes: number },
  void,
  { rejectValue: { message: string } }
>("download/downloadApk", async (_, { rejectWithValue }) => {
  logger.info("download", "initiating apk download", { url: APP.sourceApk });
  let sizeBytes: number = APP.sizeBytes;
  try {
    const head = await fetch(APP.sourceApk, { method: "HEAD" });
    if (!head.ok) {
      logger.error("download", "apk head check failed", { status: head.status });
      return rejectWithValue({
        message: `APK is not available on the server (HTTP ${head.status}).`,
      });
    }
    const headerSize = Number(head.headers.get("content-length") ?? 0);
    if (headerSize > 0) {
      sizeBytes = headerSize;
    }
    logger.info("download", "apk verified, handing off to browser", {
      sizeBytes,
    });
  } catch (err) {
    logger.error("download", "network error during head check", err);
    return rejectWithValue({
      message: "Network error while contacting the download server.",
    });
  }
  return { sizeBytes };
});

const downloadSlice = createSlice({
  name: "download",
  initialState,
  reducers: {
    resetDownload: (state) => {
      state.status = "idle";
      state.error = null;
      state.startedAt = null;
    },
    markDownloaded: (
      state,
      action: PayloadAction<{ fileName: string; sizeBytes: number }>
    ) => {
      state.status = "success";
      state.fileName = action.payload.fileName;
      state.sizeBytes = action.payload.sizeBytes;
      state.startedAt = Date.now();
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(downloadApk.pending, (state) => {
        state.status = "downloading";
        state.error = null;
        state.startedAt = Date.now();
      })
      .addCase(downloadApk.fulfilled, (state, action) => {
        state.status = "success";
        state.sizeBytes = action.payload.sizeBytes;
      })
      .addCase(downloadApk.rejected, (state, action) => {
        state.status = "error";
        state.error =
          action.payload?.message ?? "Download failed for an unknown reason.";
        state.startedAt = null;
      });
  },
});

export const { resetDownload, markDownloaded } = downloadSlice.actions;
export default downloadSlice.reducer;
