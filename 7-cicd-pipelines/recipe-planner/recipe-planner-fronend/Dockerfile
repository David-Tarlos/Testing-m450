# Testing Environment: der React-Dev-Server, nicht der Produktionsbuild.
# Fuer Produktion wuerde man "npm run build" machen und die statischen Dateien
# von einem nginx ausliefern - hier soll die Umgebung aber dem entsprechen,
# womit getestet wird.

FROM node:18-alpine
WORKDIR /app

# Es liegt kein package-lock.json bei, deshalb "npm install" statt "npm ci".
COPY package.json ./
RUN npm install

COPY public ./public
COPY src ./src

ENV HOST=0.0.0.0
# Der Dev-Server soll im Container nicht auf eine Tastatureingabe warten.
ENV CI=true
ENV WATCHPACK_POLLING=true

EXPOSE 3000
CMD ["npm", "start"]
