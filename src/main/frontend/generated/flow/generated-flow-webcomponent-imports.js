import { injectGlobalWebcomponentCss } from 'Frontend/generated/jar-resources/theme-util.js';

import '@vaadin/polymer-legacy-adapter/style-modules.js';
import '@vaadin/vertical-layout/theme/lumo/vaadin-vertical-layout.js';
import '@vaadin/app-layout/theme/lumo/vaadin-app-layout.js';
import '@vaadin/horizontal-layout/theme/lumo/vaadin-horizontal-layout.js';
import '@vaadin/button/theme/lumo/vaadin-button.js';
import '@vaadin/tooltip/theme/lumo/vaadin-tooltip.js';
import 'Frontend/generated/jar-resources/disableOnClickFunctions.js';
import '@vaadin/text-field/theme/lumo/vaadin-text-field.js';
import '@vaadin/combo-box/theme/lumo/vaadin-combo-box.js';
import 'Frontend/generated/jar-resources/flow-component-renderer.js';
import 'Frontend/generated/jar-resources/comboBoxConnector.js';
import '@vaadin/multi-select-combo-box/theme/lumo/vaadin-multi-select-combo-box.js';
import '@vaadin/integer-field/theme/lumo/vaadin-integer-field.js';
import '@vaadin/grid/theme/lumo/vaadin-grid.js';
import '@vaadin/grid/theme/lumo/vaadin-grid-column.js';
import '@vaadin/grid/theme/lumo/vaadin-grid-sorter.js';
import '@vaadin/checkbox/theme/lumo/vaadin-checkbox.js';
import 'Frontend/generated/jar-resources/gridConnector.ts';
import 'Frontend/generated/jar-resources/vaadin-grid-flow-selection-column.js';
import '@vaadin/grid/theme/lumo/vaadin-grid-column-group.js';
import 'Frontend/generated/jar-resources/lit-renderer.ts';
import '@vaadin/context-menu/theme/lumo/vaadin-context-menu.js';
import 'Frontend/generated/jar-resources/contextMenuConnector.js';
import 'Frontend/generated/jar-resources/contextMenuTargetConnector.js';
import '@vaadin/notification/theme/lumo/vaadin-notification.js';
import '@vaadin/confirm-dialog/theme/lumo/vaadin-confirm-dialog.js';
import '@vaadin/text-area/theme/lumo/vaadin-text-area.js';
import '@vaadin/login/theme/lumo/vaadin-login-form.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import '@vaadin/vaadin-lumo-styles/sizing.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === '5adb254f208fa28cbe2a3a1f8af93423e731f5f5494fc0adc38063df8269f9a3') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === 'e0718dcf21e3fcd3e04c64ed00052d35e2ad6aaa5bbc4b048303712ebfdbc45f') {
    pending.push(import('./chunks/chunk-5f40239ff95ce945664f476742b479d878733ceac640cda71fdaf8b8c2d3dd1c.js'));
  }
  if (key === '2836704b6a4e5a76c4910d23a64cf07df1d060881d2c30209a76c76b08a60cf6') {
    pending.push(import('./chunks/chunk-ac6110cc4d2f60961f4ff53fc4d8d6c847ae27b3f67e9bf492f905fa48005433.js'));
  }
  if (key === '050018fefa9ceb73b5792b6ac35f20b576d16c498f821a48667b4baee83f0b18') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === '92720abaccb4e60701cc597c12129e910f4d598aacf567930627203320bc4bfe') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === 'd30cc2ba72f4a1fd678595d589cf883594c634f1885c38f47a16a38d66a08d3c') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === '5cdc53809565dd1541e66a510cf844c37f3f1b39f56afb6e6f200cf79a23532f') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === '7b3ec6a5884b2290df25b2382f1b9ded6c1196da97116e8bf137241fe62a9c1b') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === 'f3f6847b275e5852f4dd206f5a5f1a4a55a050547f215eb0d15078f4553702ff') {
    pending.push(import('./chunks/chunk-5f40239ff95ce945664f476742b479d878733ceac640cda71fdaf8b8c2d3dd1c.js'));
  }
  if (key === '8edde3f71f8c3926fe56f80e2557b4ac3628b59261b0e1f1d5d8e7ea68919341') {
    pending.push(import('./chunks/chunk-b8e4109335b87b316123789e85b9201c6ee66150f6c8dd4429366885bd41ffda.js'));
  }
  if (key === '04d070793fffe942a69f9fb0024adb6f626463cad299ac2dfeb0b9579ff1bf69') {
    pending.push(import('./chunks/chunk-cb9a8ac87107486def312b7e55cf40527c91f75b7fba70543f17398a48f9ce8c.js'));
  }
  if (key === 'e4e25cc57452a84faa24f415d749edf086e538b975389d4000bbfe64561b07ae') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === '27671b221a411991883a5c05be198f3417570d00f1b48ecafcf0bc278702b370') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === '3f7224fd048e812105929bb512f20df66eb5b97e16757565180be914b741ca06') {
    pending.push(import('./chunks/chunk-cb9a8ac87107486def312b7e55cf40527c91f75b7fba70543f17398a48f9ce8c.js'));
  }
  if (key === '1fb242aedeacf8081a8f29821ea5930c6bda0c986ebc6d791371aaec72adb0b1') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === '1418014ac070a900e4a9fbd63802aab9f8c79e92a7aabbc12dbdfb22ac79af0a') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === 'd0744cfa354c8ddf3d4d9839d3ff806f21428db9295d23b4349fa0d4327e81ca') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === '8c34a446b401b8e1c1c4de0d6d911ce3fe0a8154b77c7b1f5d615b0a692b4e65') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === 'bf39abd46404cd8820465c744185cb9609df65858323c9c9fd7460addaa3917f') {
    pending.push(import('./chunks/chunk-cc00e46ff9c34e7a8ce75740e5cf4f3ea4b3fe6a572da809764326faac862edb.js'));
  }
  if (key === '83d127163fe12e7dc4d4d3f9214c86d29e014f163e15034988f65f729fc6f32f') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === '21d47036f0bcb7c13a98ad60c6d54ef1e122d32221032b2d5bdd4ee8cfdef1e2') {
    pending.push(import('./chunks/chunk-55c3317326f5894a72c61eaeed6751c791ce109303629f47bea35a4a43511e93.js'));
  }
  if (key === 'd705ee6e209f31276ebf261d1c0ddd50c86961ccdc0e79afdae872f8cfe96497') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === 'c7e4033086b72cdba5b0dd0850c78a77e75b5cb09d4603742f4820558cdb6dcb') {
    pending.push(import('./chunks/chunk-2c3a0a72c266d0fe75cb1b0e5cfa0aa8cf9464858915fe3d6561775d151f77f2.js'));
  }
  if (key === '410e91ea7fc15a02da815c71a0e18e16fa6c0290d8d27ddd9fa5bd3c0e48a8f2') {
    pending.push(import('./chunks/chunk-1ca170a416bb16321832bf0b36874a3ad8bc76a82f77c0085c1153abc0079acd.js'));
  }
  if (key === '0373ea4e5e9f6bcc421baed8e1499948db21cdbfac252748b7004a61ef420217') {
    pending.push(import('./chunks/chunk-cb9a8ac87107486def312b7e55cf40527c91f75b7fba70543f17398a48f9ce8c.js'));
  }
  if (key === '21d1e4231e7a77200fafa933e7c53e76c60dd8c67ad85d34d44dd851b5bdf06a') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === '36410e439853fd18de1d32d7e831765020939855e3cc4deae24f30b2633eb86f') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === '8a6c7597dea2331205e856832b2238fecfc2d74ace062adfe91d56516d9219ac') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  if (key === '56f3ba8f1e17c925d1d50c7382bb2eda24f26122fbbea6fd1cc95804e6077d61') {
    pending.push(import('./chunks/chunk-72fa936cf3f9c7280561c57caecbb3396c03c57d11f92927d8d614d125385b33.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}