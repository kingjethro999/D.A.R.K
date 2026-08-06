type LogLevel = "info" | "warn" | "error" | "debug";

const PREFIX = "[D.A.R.K]";

function write(level: LogLevel, scope: string, message: string, data?: unknown): void {
  const payload: Record<string, unknown> = {
    scope,
    message,
    at: new Date().toISOString(),
  };
  if (data !== undefined) {
    payload.data = data;
  }
  const tag = `${PREFIX}:${level.toUpperCase()}`;
  switch (level) {
    case "error":
      console.error(tag, payload);
      break;
    case "warn":
      console.warn(tag, payload);
      break;
    case "debug":
      console.debug(tag, payload);
      break;
    default:
      console.info(tag, payload);
  }
}

export const logger = {
  info: (scope: string, message: string, data?: unknown) =>
    write("info", scope, message, data),
  warn: (scope: string, message: string, data?: unknown) =>
    write("warn", scope, message, data),
  error: (scope: string, message: string, data?: unknown) =>
    write("error", scope, message, data),
  debug: (scope: string, message: string, data?: unknown) =>
    write("debug", scope, message, data),
};
