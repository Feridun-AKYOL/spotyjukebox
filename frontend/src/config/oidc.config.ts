import { UserManager, WebStorageStateStore } from "oidc-client-ts";

const oidcConfig = {
  authority: "https://accounts.spotify.com",
  client_id: "3ae1e78b356a4627a6e8238bd2f00b31",
  redirect_uri: "http://127.0.0.1:5173/callback",
  post_logout_redirect_uri: "http://127.0.0.1:5173/",
  response_type: "code",
  scope: "user-read-email user-read-private playlist-read-private",
  userStore: new WebStorageStateStore({ store: window.localStorage }),
};

export const userManager = new UserManager(oidcConfig);
