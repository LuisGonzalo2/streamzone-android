feat(purchase-dialog): corregir visibilidad de logos e icono de instrucciones

Resumen

Se corrige la visibilidad de varios iconos y logos en el diálogo de compra para asegurar contraste y que los drawables muestren su color propio.

Archivos modificados

- `app/src/main/res/layout/dialog_purchase.xml`
- `app/src/main/res/drawable/ic_bank.xml`
- `app/src/main/res/drawable/ic_bill.xml`
- `app/src/main/res/drawable/ic_siren.xml`

Descripción de los cambios

- Cambios en drawables para asegurar contraste:
  - `ic_bank.xml` — usar `android:fillColor="@color/white"` para que el icono del banco contraste en el círculo morado.
  - `ic_bill.xml` — usar `android:fillColor="@color/white"` para que el icono PayPal/factura sea visible.
  - `ic_siren.xml` — usar `android:fillColor="@color/brand_pink"` para que la sirena se muestre coloreada dentro del círculo naranja.

- Cambios en layout:
  - `dialog_purchase.xml`:
    - Eliminado `android:tint="@color/white"` en `ivImportantIcon` para que el drawable muestre su propio color.
    - Añadido `android:tint="@null"` en `ivPayment1`, `ivPayment2`, `ivPayment3`, `ivPayment4` e `ivServiceLogo` para evitar tints globales que ocultaban los iconos.

Resultado

Los logos y el icono de "Instrucciones Importantes" se ven correctamente en el diálogo de compra. No se detectaron errores críticos; quedan warnings de layout relacionados con estilos de botones (recomendación de usar botones borderless en button bars).

Validación realizada

- Revisado visualmente en el IDE: los drawables muestran color y contraste esperado.
- Verificado que los recursos referenciados (`@color/white`, `@color/brand_pink`) existen.
- Se recomienda ejecutar un build local completo (Gradle) antes de merge para validar recursos en tiempo de compilación.

Pasos para probar localmente

1. Construir el proyecto desde Android Studio (Build > Make Project) o desde terminal:

```powershell
./gradlew assembleDebug
```

(En Windows PowerShell ejecuta `.\gradlew assembleDebug` desde la raíz del proyecto.)

2. Abrir el flujo que muestra el diálogo de compra y verificar:
  - Que los iconos de pago aparezcan con el color correcto.
  - Que el icono de "Instrucciones Importantes" muestre su color propio.

Checklist para reviewers

- [ ] Revisar cambios en `dialog_purchase.xml` y confirmar que las vistas no rompen el layout.
- [ ] Revisar drawables para asegurar que los colores aplicados son los esperados y accesibles.
- [ ] Ejecutar build y smoke test del diálogo en modo debug.

Notas y seguimiento

- Warnings de layout: "Buttons in button bars should be borderless" — no bloqueante para este cambio, pero es recomendable revisarlo en una tarea aparte si queremos cumplir las recomendaciones de Material.

- Si el PR va directo a `main` y la rama está protegida, crear PR hacia `develop` o la rama de integración correspondiente.

Cómo crear el Pull Request

- URL rápida (ya disponible tras push): https://github.com/LuisGonzalo2/streamzone-android/pull/new/home-implementaciones

- O usando la GitHub CLI (si tienes `gh` instalada):

```bash
# guarda este archivo como pr_body.md y luego:
gh pr create --base main --head home-implementaciones --title "feat(purchase-dialog): corregir visibilidad de logos e icono de instrucciones" --body-file pr_body.md
```

Etiquetas y reviewers sugeridos

- Labels: `enhancement`, `ui`, `needs-review`
- Reviewers: Frontend/Android lead, UX designer (para validar contraste) — añade los usuarios del equipo según corresponda.

Si quieres, puedo:
- Crear el PR por ti (requiere `gh` o acceso web) o redactar un mensaje corto para el PR descripción si prefieres resumir menos.
- Ayudarte a corregir los warnings relacionados con `buttonBarButtonStyle` en `dialog_purchase.xml`.


