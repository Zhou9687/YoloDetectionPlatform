import { createRouter, createWebHashHistory } from "vue-router";
import UploadAnnotateView from "../views/UploadAnnotateView.vue";
import DatasetBuildView from "../views/DatasetBuildView.vue";
import PredictView from "../views/PredictView.vue";
import TrainingView from "../views/TrainingView.vue";
import LoginView from "../views/LoginView.vue";
import RegisterView from "../views/RegisterView.vue";
import { isLoggedIn } from "../utils/auth";

const routes = [
  { path: "/", redirect: "/annotate" },
  { path: "/login", component: LoginView, meta: { hideShell: true, guestOnly: true } },
  { path: "/register", component: RegisterView, meta: { hideShell: true, guestOnly: true } },
  { path: "/annotate", component: UploadAnnotateView, meta: { requiresAuth: true } },
  { path: "/dataset", component: DatasetBuildView, meta: { requiresAuth: true } },
  { path: "/training", component: TrainingView, meta: { requiresAuth: true } },
  { path: "/predict", component: PredictView, meta: { requiresAuth: true } }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

router.beforeEach((to) => {
  if (to.meta?.requiresAuth && !isLoggedIn()) {
    return "/login";
  }
  if (to.meta?.guestOnly && isLoggedIn()) {
    return "/annotate";
  }
  return true;
});

export default router;
