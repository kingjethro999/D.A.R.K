"use client";

import { useCallback } from "react";
import { APP } from "@/lib/app";
import { logger } from "@/lib/logger";
import { store } from "@/store";
import {
  downloadApk,
  markDownloaded,
  resetDownload,
} from "@/store/slices/downloadSlice";
import { useAppDispatch, useAppSelector } from "@/store/hooks";

export function useDownloadApk() {
  const dispatch = useAppDispatch();
  const state = useAppSelector((s) => s.download);

  const startDownload = useCallback(async () => {
    let sizeBytes: number = APP.sizeBytes;
    try {
      const result = await dispatch(downloadApk()).unwrap();
      sizeBytes = result.sizeBytes;
      logger.info("download", "apk verified, triggering browser save", {
        fileName: APP.downloadFileName,
        sizeBytes,
      });
    } catch {
      logger.error("download", "apk download aborted before save", {
        fileName: APP.downloadFileName,
      });
      return;
    }

    try {
      const anchor = document.createElement("a");
      anchor.href = APP.sourceApk;
      anchor.download = APP.downloadFileName;
      anchor.rel = "noopener";
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      dispatch(markDownloaded({ fileName: APP.downloadFileName, sizeBytes }));
      logger.info("download", "download started in browser", {
        fileName: APP.downloadFileName,
      });
    } catch (err) {
      logger.error("download", "failed to trigger browser save", err);
      store.dispatch(resetDownload());
    }
  }, [dispatch]);

  const reset = useCallback(() => {
    dispatch(resetDownload());
  }, [dispatch]);

  return { state, startDownload, reset };
}
