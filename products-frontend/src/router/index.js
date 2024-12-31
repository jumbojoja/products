import { createRouter, createWebHistory } from 'vue-router'
import ProductVue from '@/components/Product.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/product'
    },
    {
      path: '/product',
      component: ProductVue
    }
  ]
})

export default router
