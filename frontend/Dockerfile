FROM node:18-alpine AS build
WORKDIR /app

# Кешируем зависимости (они редко меняются)
COPY package*.json ./
RUN npm ci

# Копируем всё остальное (vite.config.ts, tsconfig, src, public...)
COPY . .

# Собираем виджет. У тебя в package.json: tsc -b && vite build && ... 
RUN npm run build

# В папке dist лежит widget.js и, возможно, index.html.
# Нам нужен только widget.js
FROM nginx:alpine

# Убираем дефолтную статику nginx
RUN rm -rf /usr/share/nginx/html/*

# Копируем только готовый виджет и index.html (если он нужен как лендинг)
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]