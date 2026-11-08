import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useAppStore = defineStore('app', () => {
  const appName = ref('workbench-perf-a9');
  const backendStatus = ref('UNKNOWN');

  function setBackendStatus(status: string): void {
    backendStatus.value = status;
  }

  return { appName, backendStatus, setBackendStatus };
});
