# Validaciones Propuestas

## 1. Validaciones de Fichero
1. **Delimitador, cita y escape**  
   Asegurar que el delimitador, el carácter de cita y el escape coinciden con los definidos en `file_configuration`.
2. **Cabecera y columnas constantes**  
   Comprobar que la cabecera está presente si `has_header=true` y que el número de columnas no varía entre filas.

## 2. Validaciones de Tipado
1. **Tipo básico**  
   Comprobar que cada campo se ajusta a su tipo: entero, decimal, fecha, timestamp o texto.
2. **Nulos y no nulos**  
   Verificar que las columnas `nullable=false` no contienen valores nulos.
3. **Longitud máxima**  
   Asegurar que los valores de CHAR/VARCHAR no exceden la longitud permitida.
4. **Formato de texto**  
   Confirmar que los campos de texto no incluyen el delimitador o comillas sin el escape correspondiente.

## 3. Validaciones de Integridad Referencial
1. **Clave primaria única**  
   Verificar unicidad de las posiciones marcadas `pk=true` en `semantic_layer`.


## 4. Validaciones Funcionales
1. **Formato de identificadores**  
   Ej. `account_number` debe tener exactamente 10 caracteres alfanuméricos.
2. **Rangos numéricos**  
   Validar que `credit_score` esté entre 300 y 850, `risk_score` entre 0 y 100, etc.
3. **Fechas lógicas**  
   Confirmar que `date_of_birth` no sea futura y que `created_date` ≥ `date_of_birth`.
4. **Validación de contacto**
    - **Email**: formato básico con regex.
    - **Teléfono**: sólo dígitos, longitud y prefijo internacional correctos.
5. **Estados y categorías válidas**  
   Verificar que campos como `account_type` o `status` estén dentro del conjunto permitido.

### Validaciones Funcionales (Cross-Field)


2. **Edad mínima**
   - **`date_of_birth` indica ≥ 18 años**  
     `(current_date() − date_of_birth) ≥ 18 años`

3. **Balance y estado**
   - **Si `status = 'Active'` entonces `balance ≥ 0`**  
     Una cuenta activa no puede tener saldo negativo.
   - **Si `status = 'Closed'` entonces `balance = 0`**  
     Las cuentas cerradas deben haberse saldado.

4. **Tipo de cuenta vs interés**
   - **Si `account_type = 'Checking'` entonces `interest_rate = 0`**  
     Las cuentas corrientes no devengan interés.

5. **Límites de descubierto**
   - **`overdraft_limit ≥ 0` y, si `overdraft_limit > 0`, entonces `status = 'Active'`**  
     Solo las cuentas activas pueden tener un descubierto.

6. **Rangos de puntuaciones**
   - **`credit_score` entre 300 y 850**
   - **`risk_score` entre 0 y 100**

7. **Transacciones para cuentas conjuntas**
   - **Si `is_joint_account = 'Yes'` entonces `num_transactions ≥ 2`**  
     Una cuenta mancomunada debería tener al menos dos titulares activos (reflejado aquí como ≥ 2 transacciones).

8. **Importe medio vs número de transacciones**
   - **Si `num_transactions = 0` entonces `avg_transaction_amount = 0`**  
     No puede haber importe medio si no hubo operaciones.
