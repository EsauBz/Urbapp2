package fr.turfu.urbapp2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import fr.turfu.urbapp2.DB.Element;
import fr.turfu.urbapp2.DB.ElementBDD;
import fr.turfu.urbapp2.DB.PixelGeom;
import fr.turfu.urbapp2.DB.Project;
import fr.turfu.urbapp2.DB.ProjectBDD;
import fr.turfu.urbapp2.Tools.DrawView;

/**
 * Created by Laura on 10/10/2016.
 */
public class ElementDefinitionActivity extends AppCompatActivity {

    /**
     * Id du projet ouvert
     */
    private long project_id;

    /**
     * Id de la photo ouverte
     */
    private long photo_id;

    /**
     * Path de la photo ouverte
     */
    private String photo_path;

    /**
     * Menu item pour le nom du projet
     */
    private MenuItem mi;

    /**
     * Booleen vrai si l'utilisateur à selectionner le crayon pour dessiner une zone
     */
    public static boolean pen;

    /**
     * Bouton crayon
     */
    private Button bPen;

    /**
     * Bitmap contenant la photo
     */
    private Bitmap photo;

    /**
     * Zone de dessin
     */
    private DrawView v;

    /*
     * Liste des pixelGeom de la photo
     */
    public static ArrayList<PixelGeom> polygones;

    /**
     * Liste des elements de la photo
     */
    public static ArrayList<Element> elements;


    /**
     * Listes des ajouts non enregistrés:
     * - newPoints : derniers points ajoutés, formant une ligne non fermée (pas encore polygone)
     * - newPolygones : dès que les points forment un polygone valide, on ajoute un polygone ici
     * - newElements : à chaque fois qu'un polygone est caractérisé, il est ajouté ici
     */
    public static ArrayList<PixelGeom> newPolygones;
    public static ArrayList<Element> newElements;
    public static ArrayList<Point> newPoints;
    public static ArrayList<String> actions; // Liste des actions faites par l'utilisateur, cette liste sert au bon fonctionnement des boutons annuler

    /**
     * Création de l'activité
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Creation
        super.onCreate(savedInstanceState);

        //Mise en place du layout
        setContentView(R.layout.activity_element_definition);

        //Paramètres
        final Intent intent = getIntent();
        photo_path = intent.getStringExtra("photo_path"); // Project ID
        project_id = intent.getLongExtra("project_id", 0);
        photo_id = intent.getLongExtra("photo_id", 0);

        //Initialisation
        actions = new ArrayList<>();
        newElements = new ArrayList<>();
        newPoints = new ArrayList<>();
        newPolygones = new ArrayList<>();
        ElementBDD ebdd = new ElementBDD(ElementDefinitionActivity.this);
        ebdd.open();
        elements = ebdd.getElement(photo_id);
        polygones = ebdd.getPixelGeom(elements);
        ebdd.close();

        //Photo
        File imgFile = new File(Environment.getExternalStorageDirectory(), photo_path);
        photo = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        RelativeLayout l = (RelativeLayout) findViewById(R.id.layoutBitmap);
        Drawable d = new BitmapDrawable(getResources(), photo);
        l.setBackground(d);
        int h = photo.getHeight();
        int w = photo.getWidth();
        v = new DrawView(ElementDefinitionActivity.this, w, h, photo_id);
        l.addView(v);

        //Mise en place de la toolbar
        Toolbar mainToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mainToolbar.setTitle("");
        mainToolbar.setSubtitle("");

        //Bouton aide
        Button help = (Button) findViewById(R.id.buttonHelp);
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                AlertDialog.Builder builder = new AlertDialog.Builder(ElementDefinitionActivity.this);

                //Layout
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.help_element_pop_up, null);
                builder.setView(dialogView);

                //Bouton close
                Button closeBtn = (Button) dialogView.findViewById(R.id.btn_close_pop);

                final AlertDialog dialog = builder.create();

                closeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });

        //Bouton crayon
        pen = false;
        bPen = (Button) findViewById(R.id.buttonPen);
        bPen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                pen = !pen;

                if (pen) {
                    Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.pen_over);
                    Drawable d = new BitmapDrawable(getResources(), b);
                    bPen.setBackground(d);
                } else {
                    Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.pen);
                    Drawable d = new BitmapDrawable(getResources(), b);
                    bPen.setBackground(d);
                }
            }
        });

        //Bouton caractériser surface
        Button carac = (Button) findViewById(R.id.buttonDefine);
        carac.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                // On trouve le pixelGeom selectionné
                int cpt = 0;
                PixelGeom select = null;

                for(PixelGeom p:polygones){
                    if(p.selected){
                        cpt++;select = p;
                    }
                }
                for(PixelGeom p:newPolygones){
                    if(p.selected){
                        cpt++; select = p;
                    }
                }

                // Si 0 ou plus de une zone sont selectionnées, on affiche une erreur, sinon, on lance la pop-up
                if(cpt==0 || cpt>1){
                    Toast.makeText(ElementDefinitionActivity.this, R.string.one_zone, Toast.LENGTH_SHORT).show();
                }else{
                    Intent intent = new Intent(ElementDefinitionActivity.this, ElementDefinitionPopUp.class);
                    intent.putExtra("photo_id",photo_id);
                    intent.putExtra("pixelGeom_id",select.getPixelGeomId());
                    actions.add("ELEMENT");
                    startActivity(intent);
                }

            }
        });

    }


    /**
     * Method to handle the clicks on the items of the toolbar
     *
     * @param item the item
     * @return true if everything went good
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.home:
                return true;

            case R.id.settings:
                return true;

            case R.id.seeDetails:
                Project p = getProject();
                popUpDetails(p.getProjectName(), p.getProjectDescription());
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Method to inflate the xml menu file (Ajout des différents onglets dans la toolbar)
     *
     * @param menu the menu
     * @return true if everything went good
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        //On sérialise le fichier menu.xml pour l'afficher dans la barre de menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        //Display Username
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String u = preferences.getString("user_preference", "");
        MenuItem i = menu.findItem(R.id.connectedAs);
        i.setTitle(u);

        //Display ProjectName
        MenuItem i1 = menu.findItem(R.id.projectTitle);
        i1.setVisible(true);
        Resources res = getResources();
        String s = res.getString(R.string.project);
        Project p = getProject();
        i1.setTitle(s + " " + p.getProjectName());
        mi = i1;

        //Affichage du bouton pour voir les détails du projet
        MenuItem i2 = menu.findItem(R.id.seeDetails);
        i2.setVisible(true);

        return super.onCreateOptionsMenu(menu);
    }


    /**
     * Lancement de la pop up avec les détails du projet
     */
    public void popUpDetails(String name, String descr) {
        PopUpDetails pud = new PopUpDetails(ElementDefinitionActivity.this, name, descr, mi);
        pud.show();
    }

    /**
     * Obtenir le projet ouvert
     *
     * @return Projet ouvert
     */
    public Project getProject() {
        ProjectBDD pbdd = new ProjectBDD(ElementDefinitionActivity.this); //Instanciation de ProjectBdd pour manipuler les projets de la base de données
        pbdd.open(); //Ouverture de la base de données
        Project p = pbdd.getProjectById(project_id); // Récupération du projet
        pbdd.close(); // Fermeture de la base de données
        return p;
    }


}
