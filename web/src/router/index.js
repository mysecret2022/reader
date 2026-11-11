import Vue from "vue";
import VueRouter from "vue-router";

Vue.use(VueRouter);

const originalPush = VueRouter.prototype.push;

VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => err);
};

const routes = [
  {
    path: "/",
    name: "index",
    component: () =>
      import(/* webpackChunkName: "index" */ "../views/Index.vue")
  },
  {
    path: "/reader",
    name: "Reader",
    component: () =>
      import(/* webpackChunkName: "reader" */ "../views/Reader.vue")
  },
  {
    path: "/admin",
    name: "Admin",
    component: () =>
      import(/* webpackChunkName: "admin" */ "../views/Admin.vue")
  },
  {
    path: "/bookSourceManage",
    name: "BookSourceManage",
    component: () =>
      import(
        /* webpackChunkName: "book-source-manage" */ "../views/BookSourceManage.vue"
      )
  },
  {
    path: "/book-source-manage",
    redirect: "/bookSourceManage"
  },
  {
    path: "/:username",
    name: "UserSpace",
    component: () =>
      import(/* webpackChunkName: "admin" */ "../views/Admin.vue")
  }
];

const router = new VueRouter({
  // mode: "history",
  base: process.env.BASE_URL,
  routes
});

export default router;
